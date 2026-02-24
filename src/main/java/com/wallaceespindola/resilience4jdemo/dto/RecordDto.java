package com.wallaceespindola.resilience4jdemo.dto;

/** A single record returned by the simulated downstream source API. */
public record RecordDto(
        String externalId,
        String name,
        String category,
        String value,
        int page,
        int indexInPage
) {}
