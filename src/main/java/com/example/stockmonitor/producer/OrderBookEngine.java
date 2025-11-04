package com.example.stockmonitor.producer;

import java.util.Comparator;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import com.example.stockmonitor.model.Trade;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OrderBookEngine {
    
    private final KafkaProducer<String, String> producer;
    private final ObjectMapper mapper = new ObjectMapper();
    private final PriorityBlockingQueue<Order> buyOrders;
    private final PriorityBlockingQueue<Order> sellOrders;
    private volatile boolean running = true;

    public OrderBookEngine() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(props);
        
        buyOrders = new PriorityBlockingQueue<>(11, Comparator.comparingDouble(Order::getPrice).reversed());
        sellOrders = new PriorityBlockingQueue<>(11, Comparator.comparingDouble(Order::getPrice));
    }

    private static class Order {
        private final String orderId;
        private final String stockSymbol;
        private final double price;
        private final int volume;
        private final boolean isBuyOrder;
        private final long timestamp;

        public Order(String orderId, String stockSymbol, double price, int volume, boolean isBuyOrder) {
            this.orderId = orderId;
            this.stockSymbol = stockSymbol;
            this.price = price;
            this.volume = volume;
            this.isBuyOrder = isBuyOrder;
            this.timestamp = System.currentTimeMillis();
        }

        public double getPrice() {
            return price;
        }
    }

    public void start() {
        try {
            while (running) {
                Order newOrder = generateRandomOrder();
                addOrder(newOrder);
                Trade trade = matchOrders();
                if (trade != null) {
                    sendTradeToKafka(trade);
                }
                Thread.sleep(800);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("OrderBookEngine interrupted, shutting down...");
        } finally {
            producer.close();
        }
    }

    private void addOrder(Order order) {
        if (order.isBuyOrder) {
            buyOrders.offer(order);
        } else {
            sellOrders.offer(order);
        }
    }

    private Order generateRandomOrder() {
        Random random = new Random();
        boolean isBuyOrder = random.nextBoolean();
        String stockSymbol = "AAPL";
        double price = 100 + random.nextDouble() * 10;
        int volume = random.nextInt(100) + 1;
        return new Order(UUID.randomUUID().toString(), stockSymbol, price, volume, isBuyOrder);
    }

    private Trade matchOrders() {
        if (buyOrders.isEmpty() || sellOrders.isEmpty()) {
            return null;
        }
        Order buyOrder = buyOrders.peek();
        Order sellOrder = sellOrders.peek();
        if (buyOrder.getPrice() >= sellOrder.getPrice()) {
            buyOrders.poll();
            sellOrders.poll();
            int tradedVolume = Math.min(buyOrder.volume, sellOrder.volume);
            double tradePrice = (buyOrder.getPrice() + sellOrder.getPrice()) / 2;
            String tradeId = UUID.randomUUID().toString();
            return new Trade(
                tradeId,
                buyOrder.stockSymbol,
                tradePrice,
                tradedVolume,
                buyOrder.orderId,
                sellOrder.orderId,
                System.currentTimeMillis()
            );
        }
        return null;
    }

    private void sendTradeToKafka(Trade trade) {
        try {
            String tradeJson = mapper.writeValueAsString(trade);
            ProducerRecord<String, String> record = new ProducerRecord<>("executed-trades", trade.getTradeId(), tradeJson);
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    System.err.println("Failed to send trade to Kafka: " + exception.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Error serializing trade: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
    }

    public static void main(String[] args) {
        OrderBookEngine engine = new OrderBookEngine();
        Runtime.getRuntime().addShutdownHook(new Thread(engine::stop));
        engine.start();
    }
}
