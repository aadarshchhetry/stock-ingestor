package com.example.stock_ingestor.model;

public record StockTick(String symbol, double price, long timestamp) {}
