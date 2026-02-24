package com.wallaceespindola.resilience4jdemo.repo;

import com.wallaceespindola.resilience4jdemo.domain.TransferRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRecordRepository extends JpaRepository<TransferRecord, Long> {

    List<TransferRecord> findByBatchId(String batchId);

    long countByBatchId(String batchId);

    @Query("SELECT DISTINCT r.batchId FROM TransferRecord r ORDER BY r.batchId DESC")
    List<String> findDistinctBatchIds();

    long countByBatchIdAndStatus(String batchId, String status);
}
