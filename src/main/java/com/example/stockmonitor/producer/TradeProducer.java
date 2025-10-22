package com.example.stockmonitor.producer;

import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import com.example.stockmonitor.model.Trade;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TradeProducer {
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
props.put(ProducerConfig.ACKS_CONFIG, "all");
props.put(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));
props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        ObjectMapper mapper = new ObjectMapper();
        Random random = new Random();
        String[] stocks = {"AAPL", "TSLA", "GOOG", "AMZN"};

        while (true) {
            Trade trade = new Trade(
                    UUID.randomUUID().toString(),
                    stocks[random.nextInt(stocks.length)],
                    100 + (500 * random.nextDouble()),
                    10 + random.nextInt(100),
                    System.currentTimeMillis()
            );
            String json = mapper.writeValueAsString(trade);
            ProducerRecord<String, String> record = new ProducerRecord<>("trades", trade.getStockSymbol(), json);
            producer.send(record);
            System.out.println("Produced: " + json);
            Thread.sleep(1000); // 1 trade/sec
        }
    }
}