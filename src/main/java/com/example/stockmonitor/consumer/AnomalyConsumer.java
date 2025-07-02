// ✅ AnomalyConsumer.java (Final Version)
package com.example.stockmonitor.consumer;

import java.util.Properties;
import java.util.Random;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AnomalyConsumer {
    public static void main(String[] args) throws InterruptedException {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        ObjectMapper mapper = new ObjectMapper();  
        Random rand = new Random();
        String[] symbols = {"AAPL", "TSLA", "GOOG", "AMZN"};

        while (true) {
            String symbol = symbols[rand.nextInt(symbols.length)];
            double percentChange = -15 + (30) * rand.nextDouble();

            if (Math.abs(percentChange) > 10) {
                ObjectNode json = mapper.createObjectNode();
                json.put("symbol", symbol);
                double formattedChange = Math.round(percentChange * 100.0) / 100.0;
json.put("percentageChange", formattedChange);
json.put("message", "Anomaly Detected: " + symbol + " price changed by " + String.format("%.2f", formattedChange) + "%");

                producer.send(new ProducerRecord<>("anomalies", json.toString()));
                System.out.println("[ANOMALY] → " + json);
            }
            Thread.sleep(3000);
        }
    }
}