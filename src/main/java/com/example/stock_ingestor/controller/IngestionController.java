package com.example.stock_ingestor.controller;

import com.example.stock_ingestor.model.StockTick;
import com.example.stock_ingestor.service.IngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class IngestionController {

    private final IngestionService service;

    public IngestionController(IngestionService service) {
        this.service = service;
    }

    @PostMapping("/ingest")
    public ResponseEntity<Void> ingest(@RequestBody List<StockTick> ticks) {
        service.ingestBatch(ticks);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/prices")
    public ResponseEntity<Object> getPrices() {
        return ResponseEntity.ok(service.getRealTimePrices());
    }
}
