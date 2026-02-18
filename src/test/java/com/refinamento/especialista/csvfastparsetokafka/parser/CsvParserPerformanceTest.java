package com.refinamento.especialista.csvfastparsetokafka.parser;

import org.junit.jupiter.api.Test;

import com.refinamento.especialista.csvfastparsetokafka.parser.CsvParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class CsvParserPerformanceTest {

    @Test
    public void testParserPerformance() throws IOException {
        String filePath = "src/main/java/com/refinamento/especialista/csvfastparsetokafka/util/books_large.csv"; // Assumes
                                                                                                                 // run
                                                                                                                 // from
                                                                                                                 // project
                                                                                                                 // root
        if (!Files.exists(Paths.get(filePath))) {
            System.out.println("File not found: " + filePath + ". Skipping performance test.");
            return;
        }

        System.out.println("Starting performance test on " + filePath);
        long startTime = System.currentTimeMillis();
        long[] count = { 0 };

        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            // Using standard Java stream for now until Gatherer is confirmed
            // available/working
            // or simply using our Gatherer implementation if we can run it.
            // Since this is a test, we can just call our Gatherer logic directly or use the
            // stream.

            // Note: gather() is a preview feature in strict sense, might need
            // --enable-preview
            // But let's assume valid environment as per pom.xml logic.

            lines.gather(CsvParser.parse())
                    .forEach(book -> {
                        count[0]++;
                        if (count[0] % 1_000_000 == 0) {
                            System.out.println("Parsed " + count[0] + " books...");
                        }
                    });
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("Finished parsing " + count[0] + " books in " + duration + " ms");
        double eps = count[0] / (duration / 1000.0);
        System.out.println("Throughput: " + String.format("%.2f", eps) + " events/sec");
    }

    @Test
    public void testParserPerformanceBytes() throws IOException {
        String filePath = "src/main/java/com/refinamento/especialista/csvfastparsetokafka/util/books_large.csv"; // Assumes
                                                                                                                 // run
                                                                                                                 // from
                                                                                                                 // project
                                                                                                                 // root
        if (!Files.exists(Paths.get(filePath))) {
            System.out.println("File not found: " + filePath + ". Skipping performance test.");
            return;
        }

        System.out.println("Starting performance test on " + filePath);
        long startTime = System.currentTimeMillis();
        long[] count = { 0 };

        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            // Using standard Java stream for now until Gatherer is confirmed
            // available/working
            // or simply using our Gatherer implementation if we can run it.
            // Since this is a test, we can just call our Gatherer logic directly or use the
            // stream.

            // Note: gather() is a preview feature in strict sense, might need
            // --enable-preview
            // But let's assume valid environment as per pom.xml logic.

            lines.gather(CsvParser.parseToBytes())
                    .forEach(book -> {
                        count[0]++;
                        if (count[0] % 1_000_000 == 0) {
                            System.out.println("Parsed " + count[0] + " books...");
                        }
                    });
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("Finished parsing " + count[0] + " books in " + duration + " ms");
        double eps = count[0] / (duration / 1000.0);
        System.out.println("Throughput: " + String.format("%.2f", eps) + " events/sec");
    }
}
