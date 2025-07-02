package com.example.stockmonitor.model;

public class Trade {
    private String tradeId;
    private String stockSymbol;
    private double price;
    private int volume;
    private long timestamp;

    public Trade() {}

    public Trade(String tradeId, String stockSymbol, double price, int volume, long timestamp) {
        this.tradeId = tradeId;
        this.stockSymbol = stockSymbol;
        this.price = price;
        this.volume = volume;
        this.timestamp = timestamp;
    }

    public String getTradeId() { return tradeId; }
    public String getStockSymbol() { return stockSymbol; }
    public double getPrice() { return price; }
    public int getVolume() { return volume; }
    public long getTimestamp() { return timestamp; }

    public void setTradeId(String tradeId) { this.tradeId = tradeId; }
    public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }
    public void setPrice(double price) { this.price = price; }
    public void setVolume(int volume) { this.volume = volume; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return String.format("Trade[%s] %s: $%.2f (%d shares)", tradeId, stockSymbol, price, volume);
    }
}
