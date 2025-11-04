package com.example.stockmonitor.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public class Trade {
    private String tradeId;
        @JsonAlias({"symbol"}) 
    private String stockSymbol; // ✅ consistent naming
    private double price;
    private int volume;
    private String buyOrderId;
    private String sellOrderId;
    private long timestamp;

    public Trade() {}

    public Trade(String tradeId, String stockSymbol, double price, int volume,
                 String buyOrderId, String sellOrderId, long timestamp) {
        this.tradeId = tradeId;
        this.stockSymbol = stockSymbol;
        this.price = price;
        this.volume = volume;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.timestamp = timestamp;
    }

    // ---------- Getters ----------
    public String getTradeId() { return tradeId; }
    public String getStockSymbol() { return stockSymbol; }
    public double getPrice() { return price; }
    public int getVolume() { return volume; }
    public String getBuyOrderId() { return buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public long getTimestamp() { return timestamp; }

    // ---------- Setters ----------
    public void setTradeId(String tradeId) { this.tradeId = tradeId; }
    public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }
    public void setPrice(double price) { this.price = price; }
    public void setVolume(int volume) { this.volume = volume; }
    public void setBuyOrderId(String buyOrderId) { this.buyOrderId = buyOrderId; }
    public void setSellOrderId(String sellOrderId) { this.sellOrderId = sellOrderId; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return String.format(
            "Trade [ID=%s | Symbol=%s | Price=₹%.2f | Volume=%d | BuyOrder=%s | SellOrder=%s | Time=%d]",
            tradeId, stockSymbol, price, volume, buyOrderId, sellOrderId, timestamp
        );
    }
}
