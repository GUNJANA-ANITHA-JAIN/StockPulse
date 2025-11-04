package com.example.stockmonitor.consumer;

import java.nio.file.Files;
import java.nio.file.Paths;
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

import io.github.cdimascio.dotenv.Dotenv;

public class ReportConsumer {

    private static final Map<String, Integer> volumeMap = new HashMap<>();
    private static final long REPORT_INTERVAL_MS = 30_000;

    public static void main(String[] args) {

        // ✅ Try multiple possible .env locations without crashing
        Dotenv dotenv = null;
        String[] possibleEnvPaths = {
            "D:/StockPulse/stock-monitoring-system/web-dashboard",
            "D:/StockPulse/stock-monitoring-system/web-dashboard/",
            "./web-dashboard",
            "../web-dashboard"
        };

        for (String p : possibleEnvPaths) {
            try {
                if (Files.exists(Paths.get(p, ".env"))) {
                    dotenv = Dotenv.configure().directory(p).load();
                    System.out.println("✅ .env loaded from: " + p);
                    break;
                }
            } catch (Exception ignored) {}
        }

        if (dotenv == null) {
            System.out.println("⚠️ .env not found, checking system environment...");
        }

        // ✅ Try to read MONGOURI safely
        String mongoUri =
            System.getenv("MONGOURI") != null
                ? System.getenv("MONGOURI")
                : dotenv != null ? dotenv.get("MONGOURI") : null;

        if (mongoUri == null || mongoUri.isEmpty()) {
            System.out.println("⚠️ MONGOURI not found. Running WITHOUT MongoDB...");
        }

        // ✅ Kafka consumer config
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "report-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        // ✅ Kafka producer config
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        ObjectMapper mapper = new ObjectMapper();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
             KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);
             MongoClient mongoClient = (mongoUri != null) ? MongoClients.create(mongoUri) : null)
        {
            MongoCollection<Document> reportCollection = null;
            if (mongoClient != null) {
                MongoDatabase db = mongoClient.getDatabase("stockpulse");
                reportCollection = db.getCollection("reports");
            }

            consumer.subscribe(Collections.singletonList("executed-trades"));
            long lastReportTime = System.currentTimeMillis();

            System.out.println("✅ ReportConsumer started — generating Top-5 summaries...");

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, String> r : records) {
                    try {
                        Trade t = mapper.readValue(r.value(), Trade.class);
                        volumeMap.merge(t.getStockSymbol(), t.getVolume(), Integer::sum);
                    } catch (Exception e) {
                        System.err.println("⚠️ Parse error: " + e.getMessage());
                    }
                }

                long now = System.currentTimeMillis();
                if (now - lastReportTime >= REPORT_INTERVAL_MS && !volumeMap.isEmpty()) {
                    sendTop5(producer, reportCollection, mapper);
                    lastReportTime = now;
                }

                consumer.commitSync();
            }

        } catch (Exception e) {
            System.err.println("💥 Fatal error in ReportConsumer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void sendTop5(KafkaProducer<String, String> producer,
                                 MongoCollection<Document> reportCollection,
                                 ObjectMapper mapper) {

        List<Map.Entry<String, Integer>> top5 = volumeMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        Map<String, Object> payload = new HashMap<>();
        payload.put("top5", top5.stream().map(e -> Map.of(
                "stock", e.getKey(),
                "volume", e.getValue()
        )).collect(Collectors.toList()));
        payload.put("timestamp", Instant.now().toString());

        try {
            String json = mapper.writeValueAsString(payload);

            producer.send(new ProducerRecord<>("reports", "{\"reset\":true}"));
            producer.send(new ProducerRecord<>("reports", json));

            if (reportCollection != null) reportCollection.insertOne(Document.parse(json));

            System.out.println("📊 Sent JSON summary: " + json);

        } catch (Exception ex) {
            System.err.println("JSON error: " + ex.getMessage());
        }

        volumeMap.clear();
    }
}
