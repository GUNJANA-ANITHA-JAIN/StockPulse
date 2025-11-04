package com.example.stockmonitor.model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Order {
    public enum Side { BUY, SELL }

    private String id;
    private String stockSymbol; // ✅ consistent with Trade.java
    private Side side;
    private double price;
    private int quantity;
    private long timestamp;

    public Order() {}

    public Order(String id, String stockSymbol, Side side, double price, int quantity, long timestamp) {
        this.id = id;
        this.stockSymbol = stockSymbol;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = timestamp;
    }

    // ---------- Getters and Setters ----------
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStockSymbol() { return stockSymbol; }
    public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }

    public Side getSide() { return side; }
    public void setSide(Side side) { this.side = side; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date(timestamp));
        String orderIdShort = id != null && id.length() > 6 ? id.substring(0, 6) : id;

        return String.format(
            "[%s] %-6s | %-8s | Price: ₹%-8.2f | Qty: %-5d | ID: %s",
            time,
            stockSymbol,
            (side == Side.BUY ? "🟢 BUY" : "🔴 SELL"),
            price,
            quantity,
            orderIdShort
        );
    }
}
