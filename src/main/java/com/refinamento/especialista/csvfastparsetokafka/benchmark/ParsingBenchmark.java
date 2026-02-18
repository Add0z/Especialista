package com.refinamento.especialista.csvfastparsetokafka.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import com.refinamento.especialista.csvfastparsetokafka.parser.CsvParser;

public class ParsingBenchmark {

    public static void main(String[] args) throws Exception {

        Path path = Paths.get("src/main/java/com/refinamento/especialista/csvfastparsetokafka/util/books_large.csv");

        // Warmup
        try (Stream<String> lines = Files.lines(path)) {
            lines.map(CsvParser::parseLineBytes).count();
        }

        System.out.println("Warmup complete.");

        long start = System.nanoTime();

        long count;
        try (Stream<String> lines = Files.lines(path)) {
            count = lines.map(CsvParser::parseLineBytes).count();
        }

        long end = System.nanoTime();

        long timeMs = (end - start) / 1_000_000;

        System.out.println("Records: " + count);
        System.out.println("Time ms: " + timeMs);
        System.out.println("Records/sec: " + (count * 1000 / timeMs));
    }
}
