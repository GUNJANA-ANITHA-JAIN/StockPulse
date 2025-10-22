package com.example.stockmonitor.producer;

import java.util.Properties;
import java.util.Random;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TradeLoadSimulator {
    public static void main(String[] args) {
        // 1. Kafka producer configuration
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            ObjectMapper mapper = new ObjectMapper();
            Random random = new Random();
           String[] stocks = {
    "AAPL",  // Apple Inc.
    "GOOGL", // Alphabet Inc. (Google)
    "TSLA",  // Tesla Inc.
    "AMZN",  // Amazon.com Inc.
    "MSFT",  // Microsoft Corporation
    "NVDA",  // NVIDIA Corporation
    "META",  // Meta Platforms Inc. (Facebook)
    "NFLX",  // Netflix Inc.
    "JPM",   // JPMorgan Chase & Co.
    "BABA"   // Alibaba Group Holding Limited
};

            for (int i = 0; i < 1000; i++) {
                ObjectNode trade = mapper.createObjectNode();

                String symbol = stocks[random.nextInt(stocks.length)];
                double price = 100 + random.nextDouble() * 500;
                int volume = random.nextInt(100) + 1;

                trade.put("stockSymbol", symbol);
                trade.put("price", price);
                trade.put("volume", volume);

                String json = trade.toString();

                ProducerRecord<String, String> record = new ProducerRecord<>("trades", symbol, json);
                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        System.err.println("❌ Error sending trade: " + exception.getMessage());
                    }
                });

                if (i % 100 == 0) {
                    System.out.println("📦 Sent " + i + " trade messages...");
                }
            }

            producer.flush();
            System.out.println("✅ 1000 trade messages sent.");
        } catch (Exception e) {
            System.err.println("🔥 Simulator failed: " + e.getMessage());
        }
    }
}