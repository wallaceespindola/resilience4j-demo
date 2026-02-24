package com.wallaceespindola.resilience4jdemo.dto;

/** Parameters for starting a bulk transfer. */
public record TransferRequest(
        int totalRecords,
        int pageSize
) {
    public TransferRequest {
        if (totalRecords <= 0) throw new IllegalArgumentException("totalRecords must be > 0");
        if (pageSize <= 0 || pageSize > 500) throw new IllegalArgumentException("pageSize must be 1–500");
    }
}
