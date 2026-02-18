package com.refinamento.especialista.csvfastparsetokafka.util;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ParquetToCsv {

    private static final Logger log = LoggerFactory.getLogger(ParquetToCsv.class);
    private static final String DEFAULT_INPUT = "/Users/andrepereira/workspace/dataSets/Goodreads Books/books_clean.parquet";
    private static final String DEFAULT_OUTPUT = "src/main/java/com/refinamento/especialista/csvfastparsetokafka/util/books_large.csv";
    // Target 2GB
    private static final long TARGET_SIZE_BYTES = 2L * 1024 * 1024 * 1024;

    public static void main(String[] args) {
        String inputPath = args.length > 0 ? args[0] : DEFAULT_INPUT;
        String outputPath = args.length > 1 ? args[1] : DEFAULT_OUTPUT;

        log.info("Starting conversion. Input: {}, Output: {}", inputPath, outputPath);
        long startTime = System.currentTimeMillis();

        if (!Files.exists(Paths.get(inputPath))) {
            log.error("Input file not found: {}", inputPath);
            return;
        }

        List<String> cache = new ArrayList<>();
        int maxCacheLines = 50000; // Limit memory usage

        // Hadoop configuration requires this workaround on some systems to ignore
        // winutils if irrelevant
        System.setProperty("hadoop.home.dir", "/tmp");

        try (ParquetReader<GenericRecord> reader = AvroParquetReader
                .<GenericRecord>builder(new Path(inputPath))
                .withConf(new Configuration())
                .build();
                BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {

            GenericRecord record;
            boolean schemaPrinted = false;

            // Read initial batch to cache
            while ((record = reader.read()) != null && cache.size() < maxCacheLines) {
                if (!schemaPrinted) {
                    log.info("Schema: {}", record.getSchema());
                    schemaPrinted = true;
                    // Write header
                    List<String> headers = new ArrayList<>();
                    for (Schema.Field field : record.getSchema().getFields()) {
                        headers.add(field.name());
                    }
                    writer.write(String.join(",", headers));
                    writer.newLine();
                }

                String csvLine = convertToCsv(record);
                if (csvLine != null) {
                    cache.add(csvLine);
                    writer.write(csvLine);
                    writer.newLine();
                }
            }

            log.info("Cached {} lines from source.", cache.size());

            if (cache.isEmpty()) {
                log.error("No data found in parquet file.");
                return;
            }

            long currentBytes = Files.size(Paths.get(outputPath));
            long linesWritten = cache.size();

            while (currentBytes < TARGET_SIZE_BYTES) {
                for (String line : cache) {
                    writer.write(line);
                    writer.newLine();
                    // Approx bytes added
                    currentBytes += line.length() + 1;
                    linesWritten++;

                    if (linesWritten % 1_000_000 == 0) {
                        log.info("Progress: {} lines, {} MB", linesWritten, currentBytes / (1024 * 1024));
                    }
                    if (currentBytes >= TARGET_SIZE_BYTES) {
                        break;
                    }
                }
            }

            long endTime = System.currentTimeMillis();
            log.info("Finished! Written {} GB to {} in {} ms. Total lines: {}",
                    currentBytes / (1024.0 * 1024 * 1024), outputPath, (endTime - startTime), linesWritten);

        } catch (IOException e) {
            log.error("Error processing parquet file", e);
        }
    }

    private static String convertToCsv(GenericRecord record) {
        StringBuilder sb = new StringBuilder();
        List<Schema.Field> fields = record.getSchema().getFields();
        for (int i = 0; i < fields.size(); i++) {
            Object value = record.get(fields.get(i).name());
            if (value != null) {
                String strVal = value.toString();
                // Escape logic
                if (strVal.contains(",") || strVal.contains("\"") || strVal.contains("\n")) {
                    sb.append("\"").append(strVal.replace("\"", "\"\"")).append("\"");
                } else {
                    sb.append(strVal);
                }
            }
            if (i < fields.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
}
