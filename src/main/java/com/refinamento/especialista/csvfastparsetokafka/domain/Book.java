package com.refinamento.especialista.csvfastparsetokafka.domain;

public record Book(
        String id,
        String name,
        String author,
        String url,
        String genres,
        String summary,
        Integer pubYear,
        Double starRating,
        Long numRatings,
        String isbn) {
}
