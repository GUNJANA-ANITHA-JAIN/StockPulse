package com.example.stockmonitor.producer;

import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import com.example.stockmonitor.model.Trade;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TradeLoadSimulator {

    private static final String TOPIC = "executed-trades";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Random random = new Random();

    private static final String[] STOCKS = {
            "AAPL", "GOOG", "MSFT", "AMZN", "TSLA", "META", "NFLX", "NVDA", "INTC", "IBM"
    };

    public static void main(String[] args) throws Exception {

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        try {
            System.out.println("📊 Simulating trade load for 1000 trades in 20 seconds...");

            for (int i = 0; i < 1000; i++) {
                String symbol = STOCKS[random.nextInt(STOCKS.length)];
                double price = getBasePrice(symbol) + random.nextGaussian() * 5;
                int quantity = ThreadLocalRandom.current().nextInt(10, 500);
                String buyOrderId = UUID.randomUUID().toString();
                String sellOrderId = UUID.randomUUID().toString();

                Trade trade = new Trade(
                        UUID.randomUUID().toString(),
                        symbol,
                        price,
                        quantity,
                        buyOrderId,
                        sellOrderId,
                        System.currentTimeMillis()
                );

                String jsonTrade = mapper.writeValueAsString(trade);
                producer.send(new ProducerRecord<>(TOPIC, symbol, jsonTrade));

                if (i % 50 == 0) {
                    System.out.println("Sent " + i + " trades...");
                }

                Thread.sleep(20); // ~20 sec total for 1000 trades
            }

            System.out.println("✅ Trade simulation completed successfully.");

        } finally {
            producer.close(); // ✅ prevents resource leak warning
        }
    }

    // ---- Java 8 compatible switch ----
    private static double getBasePrice(String symbol) {
        switch (symbol) {
            case "AAPL":
                return 180.0;
            case "GOOG":
                return 2700.0;
            case "MSFT":
                return 310.0;
            case "AMZN":
                return 3500.0;
            case "TSLA":
                return 700.0;
            case "META":
                return 320.0;
            case "NFLX":
                return 400.0;
            case "NVDA":
                return 850.0;
            case "INTC":
                return 35.0;
            case "IBM":
                return 130.0;
            default:
                return 100.0;
        }
    }
}
