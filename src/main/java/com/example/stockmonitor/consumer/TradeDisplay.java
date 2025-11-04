package com.example.stockmonitor.consumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import com.example.stockmonitor.model.Trade;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TradeDisplay {

    private static volatile boolean running = true;

    public static void main(String[] args) {
        System.out.println("TRADE DISPLAY STARTED");

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv().getOrDefault("KAFKA_BROKER", "localhost:9092"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "trade-display-" + System.currentTimeMillis());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running = false;
            consumer.wakeup();
        }));

        consumer.subscribe(Collections.singletonList("executed-trades"));

        try {
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    try {
                        Trade trade = mapper.readValue(record.value(), Trade.class);
                        System.out.printf("%-10s %-10.2f %-10d %-12s %-10s%n", trade.getStockSymbol(), trade.getPrice(), trade.getVolume(), trade.getTradeId(), new java.util.Date(trade.getTimestamp()));
                    } catch (Exception e) {
                        System.err.println("Error processing trade record: " + e.getMessage());
                    }
                }
            }
        } catch (org.apache.kafka.common.errors.WakeupException we) {
            if (running) throw we;
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
        } finally {
            consumer.close();
            System.out.println("TradeDisplay consumer closed.");
        }
    }
}
