package com.refinamento.especialista.csvfastparsetokafka.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.refinamento.especialista.csvfastparsetokafka.domain.Book;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTemplate<String, byte[]> kafkaTemplateBytes;
    private final String topic;

    public KafkaProducerService(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaTemplate<String, byte[]> kafkaTemplateBytes,
            @Value("${app.topic.books}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTemplateBytes = kafkaTemplateBytes;
        this.topic = topic;
    }

    public void send(Book book) {
        // We send the JSON representation or just the toString() for simplicity in
        // this
        // exercise
        // In a real high-perf scenario, we'd use Avro/Protobuf bytes.
        // Here we use String to visualize easy.
        String key = book.id();
        String value = book.toString(); // Record's toString() is readable

        // Fire and forget or handle future?
        // For max throughput in ingestion, we might just fire and rely on producer
        // buffers.
        // But we should at least have a callback for errors.
        CompletableFuture<?> future = kafkaTemplate.send(topic, key, value);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send book: {} - {}", book.id(), ex.getMessage());
            }
        });
    }

    public void sendBytes(String key, byte[] bytes) {
        CompletableFuture<?> future = kafkaTemplateBytes.send(topic, key, bytes);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send book: {} - {}", key, ex.getMessage());
            }
        });
    }

    public void sendBytesFast(String key, byte[] bytes) {
        kafkaTemplateBytes.send(topic, key, bytes);
    }

    public void flush() {
        kafkaTemplateBytes.flush();
    }
}
