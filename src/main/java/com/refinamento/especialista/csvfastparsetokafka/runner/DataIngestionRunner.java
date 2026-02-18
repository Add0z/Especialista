package com.refinamento.especialista.csvfastparsetokafka.runner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.refinamento.especialista.csvfastparsetokafka.service.BookIngestionService;

import java.nio.file.Files;
import java.nio.file.Paths;

@Component
@Profile("!test") // Don't run during tests
public class DataIngestionRunner implements CommandLineRunner {

    private final BookIngestionService ingestionService;
    private static final String CSV_FILE = "src/main/java/com/refinamento/especialista/csvfastparsetokafka/util/books_large.csv";

    public DataIngestionRunner(BookIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (Files.exists(Paths.get(CSV_FILE))) {
            System.out.println("Found " + CSV_FILE + ". Starting ingestion...");
            // Run in a separate thread to not block startup if needed,
            // but for this exercise blocking is fine to see output.
            ingestionService.ingestFile(CSV_FILE);
        } else {
            System.out.println("File " + CSV_FILE + " not found. Skipping ingestion.Please generate it first.");
        }
    }
}
