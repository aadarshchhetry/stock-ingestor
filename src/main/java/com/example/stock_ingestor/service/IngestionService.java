package com.example.stock_ingestor.service;

import com.example.stock_ingestor.model.StockTick;
import com.example.stock_ingestor.repository.StockRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@EnableScheduling
@Slf4j
public class IngestionService {

    private final StockRepository repository;
    private final ConcurrentLinkedQueue<StockTick> buffer = new ConcurrentLinkedQueue<>();
    private final Map<String, Double> priceCache = new ConcurrentHashMap<>();

    public IngestionService(StockRepository repository) {
        this.repository = repository;
    }

    // PRODUCER
    public void ingestBatch(List<StockTick> ticks) {
        if (ticks == null || ticks.isEmpty()) return;

        for (StockTick tick : ticks) {
            if (tick.price() > 0) {
                buffer.add(tick);
                priceCache.put(tick.symbol(), tick.price());
            }
        }
    }

    // CONSUMER
    @Scheduled(fixedDelay = 100)
    public void flushBufferToDb() {
        if (buffer.isEmpty()) return;

        List<StockTick> batch = new ArrayList<>();
        int batchSize = 0;

        while (!buffer.isEmpty() && batchSize < 2000) {
            batch.add(buffer.poll());
            batchSize++;
        }

        if (!batch.isEmpty()) {
            repository.batchInsert(batch);
        }
    }

    public Map<String, Double> getRealTimePrices() {
        return priceCache;
    }

    @PreDestroy
    public void onShutdown() {
        log.info("Shutdown signal received! Draining buffer ->");

        int initialSize = buffer.size();

        // Keep flushing until the buffer is completely empty
        while (!buffer.isEmpty()) {
            flushBufferToDb();
        }

        log.info("Shutdown complete. Flushed {} items to Postgres.", initialSize);
    }
}
