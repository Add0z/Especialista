package com.refinamento.especialista.csvfastparsetokafka.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.refinamento.especialista.csvfastparsetokafka.infrastructure.KafkaProducerService;
import com.refinamento.especialista.csvfastparsetokafka.parser.CsvParser;
import com.refinamento.especialista.csvfastparsetokafka.parser.CsvParser.ParsedLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Gatherer;
import java.util.stream.Stream;

@Service
public class BookIngestionService {

    private static final Logger log = LoggerFactory.getLogger(BookIngestionService.class);
    private final KafkaProducerService producerService;

    public BookIngestionService(KafkaProducerService producerService) {
        this.producerService = producerService;
    }

    public void ingestFile(String filePath) {
        log.info("Starting ingestion of file: {}", filePath);
        long startTime = System.currentTimeMillis();
        long[] count = { 0 }; // Mutable counter for lambda

        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {

            // Using Java 25 Gatherer API (if available standard)
            // If strictly following the gather(Gatherer) syntax:
            lines.gather(CsvParser.parse())
                    .forEach(book -> {
                        producerService.send(book);
                        count[0]++;
                        if (count[0] % 100_000 == 0) {
                            log.info("Ingested {} books...", count[0]);
                        }
                    });

        } catch (IOException e) {
            log.error("Error reading file: {}", filePath, e);
        }

        long endTime = System.currentTimeMillis();
        log.info("Finished ingestion. Total books: {} in {} ms", count[0], (endTime -
                startTime));
    }

    public void ingestFileBytes(String filePath) {
        log.info("Starting ingestion of file optimized: {}", filePath);
        long startTime = System.currentTimeMillis();
        long[] count = { 0 }; // Mutable counter for lambda

        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            lines.gather(CsvParser.parseToBytes())
                    .forEach(this::kafkaSendBytes);

        } catch (IOException e) {
            log.error("Error reading file: {}", filePath, e);
        }

        long endTime = System.currentTimeMillis();
        log.info("Finished ingestion. Total books: {} in {} ms", count[0], (endTime -
                startTime));
    }

    private Gatherer<ParsedLine, ArrayList<ParsedLine>, List<ParsedLine>> batchBytes() {
        return Gatherer.ofSequential(
                ArrayList<ParsedLine>::new,
                Gatherer.Integrator.of((state, element, downstream) -> {
                    state.add(element);
                    if (state.size() >= 100_000) {
                        List<ParsedLine> batch = new ArrayList<>(state);
                        state.clear();
                        return downstream.push(batch);
                    }
                    return true;
                }),

                // 3. Finisher: Push any remaining elements when stream ends
                (state, downstream) -> {
                    if (!state.isEmpty()) {
                        downstream.push(new ArrayList<>(state));
                    }
                });
    }

    private void kafkaSendBytes(ParsedLine parsedLine) {
        producerService.sendBytes(parsedLine.key(), parsedLine.value());
    }

}
