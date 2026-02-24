package com.wallaceespindola.resilience4jdemo.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** JPA entity representing a record successfully transferred from the downstream source. */
@Entity
@Table(name = "transfer_records", indexes = {
        @Index(name = "idx_batch_id", columnList = "batchId"),
        @Index(name = "idx_external_id", columnList = "externalId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String batchId;

    @Column(nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String name;

    private String category;
    private String value;

    private int sourcePage;

    @Column(nullable = false)
    private String status; // "inserted" | "fallback"

    @Column(nullable = false)
    private Instant transferredAt;
}
