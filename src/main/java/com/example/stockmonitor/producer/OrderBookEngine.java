package com.example.stockmonitor.producer;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import com.example.stockmonitor.model.Trade;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OrderBookEngine {

    // ---- ORDER CLASS ----
    private static class Order {
        String id;
        String symbol;
        double price;
        int volume;
        boolean buy;

        Order(String id, String symbol, double price, int volume, boolean buy) {
            this.id = id;
            this.symbol = symbol;
            this.price = price;
            this.volume = volume;
            this.buy = buy;
        }
    }

    // ---- STOCKS TO SIMULATE ----
    private static final List<String> SYMBOLS = Arrays.asList("AAPL", "GOOG", "MSFT", "AMZN", "TSLA");

    // ---- OB MAPS ----
    private final Map<String, PriorityBlockingQueue<Order>> bidsMap = new HashMap<>();
    private final Map<String, PriorityBlockingQueue<Order>> asksMap = new HashMap<>();

    private final KafkaProducer<String, String> producer;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Random random = new Random();

    public OrderBookEngine() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(props);

        // create orderbooks for each symbol
        for (String sym : SYMBOLS) {
            bidsMap.put(sym, new PriorityBlockingQueue<>(11, Comparator.comparingDouble(o -> -o.price)));
            asksMap.put(sym, new PriorityBlockingQueue<>(11, Comparator.comparingDouble(o -> o.price)));
        }
    }

    public void start() throws Exception {
    while (true) {
        String symbol = SYMBOLS.get(random.nextInt(SYMBOLS.size()));

        // generate order
        Order o = new Order(
                UUID.randomUUID().toString(),
                symbol,
                basePrice(symbol) + random.nextDouble() * 5,
                (int)(1 + random.nextDouble() * random.nextDouble() * 60),
                random.nextBoolean()
        );

        addOrder(o);

        Trade t = match(symbol);
        if (t != null) sendTrade(t);

        sendOrderBookSnapshot(symbol);
        // 🕙 Slow the feed for observability (700–1200ms)
        Thread.sleep(1500 + random.nextInt(500));
    }
}


    private double basePrice(String sym) {
        switch (sym) {
            case "AAPL": return 150;
            case "GOOG": return 2800;
            case "MSFT": return 300;
            case "AMZN": return 3300;
            case "TSLA": return 700;
            default: return 100;
        }
    }

    private void addOrder(Order o) {
        if (o.buy) bidsMap.get(o.symbol).add(o);
        else asksMap.get(o.symbol).add(o);
    }

    private Trade match(String symbol) {
        PriorityBlockingQueue<Order> bids = bidsMap.get(symbol);
        PriorityBlockingQueue<Order> asks = asksMap.get(symbol);

        if (bids.isEmpty() || asks.isEmpty()) return null;

        Order b = bids.peek();
        Order s = asks.peek();

        if (b.price >= s.price) {
            bids.poll();
            asks.poll();
            int vol = Math.min(b.volume, s.volume);
            double price = (b.price + s.price) / 2;

            return new Trade(
                    UUID.randomUUID().toString(),
                    symbol,
                    price,
                    vol,
                    b.id,
                    s.id,
                    System.currentTimeMillis()
            );
        }
        return null;
    }

    private void sendTrade(Trade t) {
        try {
            String json = mapper.writeValueAsString(t);
            producer.send(new ProducerRecord<>("executed-trades", json));
        } catch (Exception ignored) {}
    }

    private List<Map<String, Object>> top(PriorityBlockingQueue<Order> q, boolean buy) {
        return q.stream()
                .sorted(buy ? Comparator.comparingDouble(o -> -o.price) : Comparator.comparingDouble(o -> o.price))
                .limit(5)
                .map(o -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("price", o.price);
                    m.put("volume", o.volume);
                    return m;
                })
                .collect(Collectors.toList());
    }

    private void sendOrderBookSnapshot(String symbol) {
        try {
            Map<String, Object> ob = new HashMap<>();
            ob.put("symbol", symbol);
            ob.put("bids", top(bidsMap.get(symbol), true));
            ob.put("asks", top(asksMap.get(symbol), false));

            String json = mapper.writeValueAsString(ob);
            producer.send(new ProducerRecord<>("orderbook", json));
        } catch (Exception e) {
            System.err.println("OrderBook send error: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        new OrderBookEngine().start();
    }
}
