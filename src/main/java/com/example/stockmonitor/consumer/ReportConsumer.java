package com.example.stockmonitor.consumer;

import java.time.Duration;
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

    public static void main(String[] args) throws Exception {
        // Kafka consumer config
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "report-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList("trades"));

        // Kafka producer config
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));

        KafkaProducer<String, String> reportProducer = new KafkaProducer<>(producerProps);
        ObjectMapper mapper = new ObjectMapper();

        // MongoDB setup
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("test");
        MongoCollection<Document> reportCollection = database.getCollection("reports");

        long lastReportTime = System.currentTimeMillis();

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            for (ConsumerRecord<String, String> record : records) {
                Trade trade = mapper.readValue(record.value(), Trade.class);
                volumeMap.merge(trade.getStockSymbol(), trade.getVolume(), Integer::sum);
            }

            long now = System.currentTimeMillis();
            if (now - lastReportTime >= 30_000 && volumeMap.size() >= 1) {
                System.out.println("\n📊 Trade Summary Report (Top 5):");

                // Step 1: Send Kafka reset signal
                reportProducer.send(new ProducerRecord<>("reports", "__RESET__", "__RESET__"));

                // Step 2: Clear MongoDB report collection
                reportCollection.deleteMany(new Document());

                // Step 3: Send Top 5 entries
                List<Map.Entry<String, Integer>> top5 = volumeMap.entrySet().stream()
                        .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                        .limit(5)
                        .collect(Collectors.toList());

                for (Map.Entry<String, Integer> entry : top5) {
                    String msg = String.format("%s: Total Volume = %d", entry.getKey(), entry.getValue());
                    System.out.println(msg);

                    // Kafka format: key = symbol, value = msg (just plain string)
                    reportProducer.send(new ProducerRecord<>("reports", entry.getKey(), msg));

                    // Also add to MongoDB
                    Document doc = new Document("value", msg);
                    reportCollection.insertOne(doc);
                }

                System.out.println("---------------------------------------------------");

                volumeMap.clear();
                lastReportTime = now;
                consumer.commitSync();
            }
        }
    }
}