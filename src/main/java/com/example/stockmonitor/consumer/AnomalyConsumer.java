package com.example.stockmonitor.consumer;

import java.util.Properties;
import java.util.Random;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AnomalyConsumer {

    private static final String BROKER = "localhost:9092";
    private static final String TOPIC = "anomalies";
    private static final String[] STOCKS = { "AAPL", "GOOG", "MSFT", "AMZN", "TSLA", "META", "NFLX", "NVDA", "INTC", "IBM"};
    private static final double THRESHOLD = 10.0; // ±10%

    public static void main(String[] args) throws InterruptedException {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BROKER);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            ObjectMapper mapper = new ObjectMapper();
            Random random = new Random();

            System.out.println("🚀 AnomalyConsumer started — monitoring stock anomalies...");

            while (true) {
                if (random.nextDouble() < 0.3) { // ~30% chance
                    String stock = STOCKS[random.nextInt(STOCKS.length)];
                    double change = -15 + random.nextDouble() * 30; // -15% to +15%

                    if (Math.abs(change) > THRESHOLD) {
                        ObjectNode json = mapper.createObjectNode();
                        json.put("stockSymbol", stock);
                        json.put("percentageChange", Math.round(change * 100.0) / 100.0);
                        json.put("message", String.format("⚠️ %s changed by %.2f%%", stock, change));

                        String payload = json.toString();
                        producer.send(new ProducerRecord<>(TOPIC, stock, payload));
                        System.out.println("[ANOMALY] " + payload);
                    }
                }
                Thread.sleep(2000);
            }
        }
    }
}
