package com.example.stock_ingestor.repository;

import com.example.stock_ingestor.model.StockTick;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public class StockRepository {

    private final JdbcTemplate jdbcTemplate;

    public StockRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS ticks (symbol VARCHAR(10), price DOUBLE, timestamp BIGINT)");
    }

    public void batchInsert(List<StockTick> ticks) {
        String sql = "INSERT INTO ticks (symbol, price, timestamp) VALUES (?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, ticks, 1000, (ps, tick) -> {
            ps.setString(1, tick.symbol());
            ps.setDouble(2, tick.price());
            ps.setLong(3, tick.timestamp());
        });
    }
}
