package com.refinamento.especialista.csvfastparsetokafka.parser;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Gatherer;

import com.refinamento.especialista.csvfastparsetokafka.domain.Book;

public class CsvParser {

    // Simple implementation for now,
    // will enhance for"allocation-free"
    // logic by
    // avoiding String.split()
    // and using index-
    // based parsing if needed.However,
    // since we return
    // a Record
    // with Strings, we
    // must allocate
    // those Strings.

    public static Gatherer<String, ?, Book> parse() {
        return Gatherer.of(
                (state, line, downstream) -> {
                    try {
                        // Skip empty lines
                        if (line.isEmpty()) {
                            return true;
                        }

                        // Manual parsing to avoid regex overhead of split()
                        // This is a "low-level" parsing demonstration
                        Book book = parseLine(line);
                        return downstream.push(book);
                    } catch (Exception e) {
                        // Log or ignore malformed lines for robustness
                        return true;
                    }
                });
    }

    private static Book parseLine(String line) {
        // We know the schema:
        // id, name, author, url, genres, summary_clean, pub_year, star_rating,
        // num_ratings, isbn_clean
        // 10 fields.

        // Primitive parser that handles quoted fields (basic)
        String[] tokens = new String[10];
        int currentToken = 0;
        int start = 0;
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                if (currentToken < 10) {
                    tokens[currentToken++] = clean(line.substring(start, i));
                }
                start = i + 1;
            }
        }
        // Last token
        if (currentToken < 10) {
            tokens[currentToken] = clean(line.substring(start));
        }

        return new Book(
                tokens[0],
                tokens[1],
                tokens[2],
                tokens[3],
                tokens[4],
                tokens[5],
                parseInteger(tokens[6]),
                parseDouble(tokens[7]),
                parseLong(tokens[8]),
                tokens[9]);
    }

    private static String clean(String s) {
        // Verifica se a string tem pelo menos "" (length 2) antes de fazer o substring
        if (s != null && s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1).replace("\"\"", "\"");
        }
        return s;
    }

    private static Integer parseInteger(String s) {
        if (s == null || s.isEmpty())
            return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isEmpty())
            return null;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long parseLong(String s) {
        if (s == null || s.isEmpty())
            return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Gatherer<String, ?, ParsedLine> parseToBytes() {
        return Gatherer.of(
                (state, line, downstream) -> {
                    try {
                        // Skip empty lines
                        if (line.isEmpty()) {
                            return true;
                        }

                        // Manual parsing to avoid regex overhead

                        // of split()
                        // This is a "low-level" parsing demonstration
                        ParsedLine parsedLine = parseLineBytes(line);
                        return downstream.push(parsedLine);
                    } catch (

                Exception e) {
                        // Log or ignore malformed lines for robustness
                        return true;
                    }
                });
    }

    public record ParsedLine(String key, byte[] value) {
    }

    public static ParsedLine parseLineBytes(String line) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(line.length());
        boolean inQuotes = false;

        String key = null;
        int firstCommaIndex = -1;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            char nextChar = (i + 1 < line.length()) ? line.charAt(i + 1) : '\0';

            // Capture key boundary (first comma outside quotes)
            if (c == ',' && !inQuotes && firstCommaIndex == -1) {
                firstCommaIndex = i;
                key = line.substring(0, i); // very small allocation
            }

            if (c == ',' && !inQuotes) {
                out.write((byte) c);
            } else if (c == '"' && inQuotes && nextChar == '"') {
                out.write((byte) c);
                i++;
            } else if (c == '"') {
                inQuotes = !inQuotes;
            } else {
                out.write((byte) c);
            }
        }

        return new ParsedLine(key, out.toByteArray());
    }

    public static ParsedLine parseLineToPseudoAvro(String line) {
        String[] tokens = parseTokens(line);

        // Adicionamos um offset de segurança para os metadados binários
        ByteBuffer buffer = ByteBuffer.allocate(line.length() + 64);

        try {
            // Garantindo valores default para evitar NullPointerException
            buffer.putLong(parseLong(tokens[0]) != null ? parseLong(tokens[0]) : 0L);
            buffer.putInt(parseInteger(tokens[6]) != null ? parseInteger(tokens[6]) : 0);
            buffer.putDouble(parseDouble(tokens[7]) != null ? parseDouble(tokens[7]) : 0.0);
            buffer.putLong(parseLong(tokens[8]) != null ? parseLong(tokens[8]) : 0L);

            for (int i : new int[] { 1, 2, 3, 4, 5, 9 }) { // Campos de texto
                writeString(buffer, tokens[i]);
            }

        } catch (Exception e) {
            // Em benchmark, é melhor logar o erro do que apenas silenciar
            return new ParsedLine("error", new byte[0]);
        }

        byte[] result = new byte[buffer.position()];
        buffer.flip();
        buffer.get(result);

        return new ParsedLine(tokens[0], result);
    }

    private static void writeString(ByteBuffer buffer, String s) {
        if (s == null) {
            buffer.putInt(0);
        } else {
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            buffer.putInt(bytes.length);
            buffer.put(bytes);
        }
    }

    private static String[] parseTokens(String line) {
        String[] tokens = new String[10];
        int currentToken = 0;
        int start = 0;
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                if (currentToken < 10) {
                    tokens[currentToken++] = clean(line.substring(start, i));
                }
                start = i + 1;
            }
        }
        if (currentToken < 10) {
            tokens[currentToken] = clean(line.substring(start));
        }
        return tokens;
    }

}
