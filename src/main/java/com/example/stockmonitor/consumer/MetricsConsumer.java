package com.example.stockmonitor.consumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class MetricsConsumer {

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "metrics-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList("executed-trades"));
            System.out.println("📊 MetricsConsumer started — tracking trade metrics...");

            long startTime = System.currentTimeMillis();
            long totalTrades = 0, perMinuteCount = 0;

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                if (!records.isEmpty()) {
    perMinuteCount += records.count();
    totalTrades += records.count();
}
                long now = System.currentTimeMillis();
                if (now - startTime >= 60_000) {
                    System.out.printf("📈 Trades/min: %-5d | Total: %-6d | Time: %tT%n",
                            perMinuteCount, totalTrades, new Date());
                    perMinuteCount = 0;
                    startTime = now;
                }
                consumer.commitSync();
            }
        } catch (Exception e) {
            System.err.println("⚠️ MetricsConsumer error: " + e.getMessage());
        }
    }
}
