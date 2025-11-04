package com.example.stockmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import com.example.stockmonitor.model.Trade;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AppTest {

    @Test
    public void testTradeSerialization() throws Exception {
        Trade trade = new Trade(
                "T001",
                "AAPL",
                150.0,
                100,
                "BUY123",
                "SELL456",
                System.currentTimeMillis()
        );

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(trade);
        assertNotNull(json);

        Trade parsed = mapper.readValue(json, Trade.class);
        assertEquals(trade.getStockSymbol(), parsed.getStockSymbol());
        assertEquals(trade.getVolume(), parsed.getVolume());
    }
}
