package com.refinamento.especialista.csvfastparsetokafka.benchmark;

import com.refinamento.especialista.JavawizardApplication;
import com.refinamento.especialista.csvfastparsetokafka.infrastructure.KafkaProducerService;
import com.refinamento.especialista.csvfastparsetokafka.parser.CsvParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class IngestionBenchmark {

    public static void main(String[] args) throws Exception {

        ConfigurableApplicationContext context = SpringApplication.run(JavawizardApplication.class);

        KafkaProducerService producerService = context.getBean(KafkaProducerService.class);

        // Limpar tópico antes do teste
        resetTopic(context);

        Path path = Paths.get("src/main/java/com/refinamento/especialista/csvfastparsetokafka/util/books_large.csv");

        // Warmup
        try (Stream<String> lines = Files.lines(path)) {
            lines.map(CsvParser::parseLineBytes).count();
        }

        System.out.println("Warmup complete.");

        long start = System.nanoTime();

        long count;
        try (Stream<String> lines = Files.lines(path)) {
            count = lines
                    .map(CsvParser::parseLineBytes)
                    .peek(parsedLine -> producerService.sendBytesFast(parsedLine.key(), parsedLine.value()))
                    .count();
        }

        producerService.flush();

        long end = System.nanoTime();

        long timeMs = (end - start) / 1_000_000;

        System.out.println("Records: " + count);
        System.out.println("Time ms: " + timeMs);
        System.out.println("Records/sec: " + (count * 1000 / timeMs));

        context.close();
    }

    private static void resetTopic(ConfigurableApplicationContext context) {
        try (var admin = AdminClient.create(
                java.util.Map.of(
                        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"))) {

            var topicName = context.getEnvironment().getProperty("app.topic.books", "books-topic");
            System.out.println("Resetting topic: " + topicName);

            try {
                admin.deleteTopics(java.util.List.of(topicName)).all().get();
                // Wait for deletion to propagate
                Thread.sleep(2000);
            } catch (Exception e) {
                System.out.println("Topic might not exist, proceeding to create...");
            }

            var newTopic = new NewTopic(topicName, 6, (short) 1);
            admin.createTopics(java.util.List.of(newTopic)).all().get();
            System.out.println("Topic reset complete.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset topic", e);
        }
    }
}
