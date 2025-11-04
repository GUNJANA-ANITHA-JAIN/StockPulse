const express = require("express");
const http = require("http");
const { Kafka } = require("kafkajs");
const socketIo = require("socket.io");

const PORT = process.env.PORT || 3001;
const BROKERS = (process.env.KAFKABROKER || "localhost:9092").split(",");


const app = express();
const server = http.createServer(app);
const io = socketIo(server, { cors: { origin: "*", methods: ["GET", "POST"] } });

app.use(express.static("public"));

let liveTrades = [];
let orderbook = null;
const kafka = new Kafka({ clientId: "stockpulse-ui", brokers: BROKERS, ssl: !!process.env.KAFKA_SSL,
  sasl: process.env.KAFKA_SASL ? {
    mechanism: process.env.KAFKA_MECH || "plain",
    username: process.env.KAFKA_USERNAME,
    password: process.env.KAFKA_PASSWORD
  } : undefined
});
const consumer = kafka.consumer({ groupId: "ui-" + Math.random() });

let lastTradeEmit = 0;
let lastOBEmit = 0;

const TRADE_INTERVAL = 300; 
const OB_INTERVAL = 500;

async function runKafka() {
  await consumer.connect();
  await consumer.subscribe({
    topics: ["executed-trades", "orderbook", "anomalies", "reports"],
    fromBeginning: false
  });

  await consumer.run({
    eachMessage: async ({ topic, message }) => {
      const data = message.value?.toString();
      if (!data) return;

      try {
        const json = JSON.parse(data);
        const now = Date.now();

        if (topic === "executed-trades") {
          liveTrades.push(json);
          if (liveTrades.length > 20) liveTrades.shift();

          if (now - lastTradeEmit > TRADE_INTERVAL) {
            io.emit("liveTradesSnapshot", liveTrades);
            lastTradeEmit = now;
          }
        }

        if (topic === "orderbook") {
          orderbook = json;
          if (now - lastOBEmit > OB_INTERVAL) {
            io.emit("orderbookUpdate", json);
            lastOBEmit = now;
          }
        }

        if (topic === "anomalies") io.emit("anomalyDetected", json);
        if (topic === "reports") io.emit("tradeSummaries", json);

      } catch {}
    }
  });

  console.log("✅ Kafka → Websocket streaming started");
}

io.on("connection", (s) => {
  console.log("👤 Client connected", s.id);
  s.emit("liveTradesSnapshot", liveTrades);
  if (orderbook) s.emit("orderbookUpdate", orderbook);
});

server.listen(PORT, () => console.log(`🌍 UI at http://localhost:${PORT}`));
runKafka();
