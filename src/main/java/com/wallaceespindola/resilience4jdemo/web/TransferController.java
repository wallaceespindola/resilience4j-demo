package com.wallaceespindola.resilience4jdemo.web;

import com.wallaceespindola.resilience4jdemo.domain.TransferRecord;
import com.wallaceespindola.resilience4jdemo.dto.ApiResponse;
import com.wallaceespindola.resilience4jdemo.dto.TransferRequest;
import com.wallaceespindola.resilience4jdemo.dto.TransferSummary;
import com.wallaceespindola.resilience4jdemo.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bulk-transfer endpoint — the main demo scenario.
 *
 * <p>A transfer fetches N records from the simulated downstream API in pages,
 * applying all six Resilience4J modules, and persists records into H2.
 * The response includes a full summary of what happened (retries, fallbacks, etc.).
 */
@RestController
@RequestMapping("/api/transfer")
@Tag(name = "Transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService service;

    /**
     * POST — Start a bulk transfer with a JSON body.
     * GET alternative: {@code /api/transfer/start/{totalRecords}/{pageSize}}
     */
    @PostMapping("/start")
    @Operation(summary = "Start bulk transfer (POST with body)")
    public ApiResponse<TransferSummary> start(@RequestBody TransferRequest request,
                                               HttpServletRequest req) {
        TransferSummary summary = service.transfer(request.totalRecords(), request.pageSize());
        return ApiResponse.ok(summary, "Transfer complete", cid(req), req.getRequestURI());
    }

    /** GET alternative — transfer {totalRecords} records with {pageSize} per page. */
    @GetMapping("/start/{totalRecords}/{pageSize}")
    @Operation(summary = "Start bulk transfer (GET with path vars)")
    public ApiResponse<TransferSummary> startGet(@PathVariable int totalRecords,
                                                  @PathVariable int pageSize,
                                                  HttpServletRequest req) {
        TransferSummary summary = service.transfer(totalRecords, pageSize);
        return ApiResponse.ok(summary, "Transfer complete", cid(req), req.getRequestURI());
    }

    @GetMapping("/history")
    @Operation(summary = "List all batch IDs")
    public ApiResponse<List<String>> history(HttpServletRequest req) {
        return ApiResponse.ok(service.listBatchIds(), cid(req), req.getRequestURI());
    }

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "Get records for a specific batch")
    public ApiResponse<List<TransferRecord>> batchRecords(@PathVariable String batchId,
                                                           HttpServletRequest req) {
        return ApiResponse.ok(service.getRecordsForBatch(batchId), cid(req), req.getRequestURI());
    }

    private String cid(HttpServletRequest req) {
        Object c = req.getAttribute("correlationId");
        return c != null ? c.toString() : "n/a";
    }
}
