package com.example.stockmonitor.consumer;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.bson.Document;

import com.example.stockmonitor.model.Trade;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class ReportConsumer {
    private static final Map<String, Integer> volumeMap = new HashMap<>();
    private static final long REPORT_INTERVAL_MS = 30_000;

    public static void main(String[] args) {
        // ===== Kafka Consumer =====
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "report-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        // ===== Kafka Producer =====
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");

        ObjectMapper mapper = new ObjectMapper();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
             KafkaProducer<String, String> reportProducer = new KafkaProducer<>(producerProps);
             MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {

            MongoDatabase database = mongoClient.getDatabase("test");
            MongoCollection<Document> reportCollection = database.getCollection("reports");

            consumer.subscribe(Collections.singletonList("executed-trades"));
            long lastReportTime = System.currentTimeMillis();
            System.out.println("✅ ReportConsumer started — tracking executed trades...");

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, String> record : records) {
                    try {
                        Trade trade = mapper.readValue(record.value(), Trade.class);
                        volumeMap.merge(trade.getStockSymbol(), trade.getVolume(), Integer::sum);
                    } catch (Exception e) {
                        System.err.println("⚠️ JSON parse error: " + e.getMessage());
                    }
                }

                long now = System.currentTimeMillis();
                if (now - lastReportTime >= REPORT_INTERVAL_MS && !volumeMap.isEmpty()) {
                    generateTop5Report(reportProducer, reportCollection);
                    lastReportTime = now;
                }

                consumer.commitSync();
            }
        } catch (Exception e) {
            System.err.println("💥 Fatal error in ReportConsumer: " + e.getMessage());
        }
    }

    private static void generateTop5Report(KafkaProducer<String, String> reportProducer,
                                           MongoCollection<Document> reportCollection) {

        System.out.println("\n📊 Top 5 Traded Stocks:");
        // Add this before reportCollection.deleteMany() to ensure the topic isn't spammed when no data changes
if (volumeMap.isEmpty()) return;

        // Clear Mongo collection (optional if you want rolling storage)
        reportCollection.deleteMany(new Document());
        reportProducer.send(new ProducerRecord<>("reports", "__RESET__", "__RESET__"));

        List<Map.Entry<String, Integer>> top5 = volumeMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        for (Map.Entry<String, Integer> entry : top5) {
            String msg = String.format("%s: Volume = %d", entry.getKey(), entry.getValue());
            System.out.println("   • " + msg);

            reportProducer.send(new ProducerRecord<>("reports", entry.getKey(), msg));
            reportCollection.insertOne(new Document("stockSymbol", entry.getKey())
                    .append("volume", entry.getValue())
                    .append("timestamp", Instant.now().toString()));
        }

        System.out.println("------------------------------------------------");
        volumeMap.clear();
    }
}
