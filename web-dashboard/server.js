const express = require("express");
const http = require("http");
const { Kafka } = require("kafkajs");
const socketIo = require("socket.io");
const mongoose = require("mongoose");

const app = express();
const server = http.createServer(app);
const io = socketIo(server);

app.use(express.static("public"));

// Connect to MongoDB
const mongoUri = "mongodb://localhost:27017/test";
mongoose.connect(mongoUri,);
const db = mongoose.connection;
db.on("error", console.error.bind(console, "❌ MongoDB connection error:"));
db.once("open", () => console.log("✅ MongoDB connected"));

const tradeSchema = new mongoose.Schema({}, { strict: false });
const anomalySchema = new mongoose.Schema({}, { strict: false });
const reportSchema = new mongoose.Schema({}, { strict: false });

const Trade = mongoose.model("Trade", tradeSchema, "trades");
const Anomaly = mongoose.model("Anomaly", anomalySchema, "anomalies");
const Report = mongoose.model("Report", reportSchema, "reports");

// Kafka Setup
const kafka = new Kafka({ clientId: "web-dashboard", 
  brokers: ["localhost:9092"] });

const tradeConsumer = kafka.consumer({ groupId: "web-dashboard-trade-group" });
const anomalyConsumer = kafka.consumer({ groupId: "web-dashboard-anomaly-group" });
const reportConsumer = kafka.consumer({ groupId: "web-dashboard-report-group" });

// Trade Consumer: Write to DB
(async () => {
  await tradeConsumer.connect();
  await tradeConsumer.subscribe({ topic: "trades", fromBeginning: false });

  await tradeConsumer.run({
    eachMessage: async ({ message }) => {
      try {
        const trade = JSON.parse(message.value.toString());
        await Trade.create(trade);
      } catch (e) {
        console.error("❌ Trade insert error:", e.message);
      }
    },
  });
})();

// Anomaly Consumer: Write to DB
(async () => {
  await anomalyConsumer.connect();
  await anomalyConsumer.subscribe({ topic: "anomalies", fromBeginning: false });

  await anomalyConsumer.run({
    eachMessage: async ({ message }) => {
      try {
        const anomaly = JSON.parse(message.value.toString());
        await Anomaly.create(anomaly);
      } catch (e) {
        console.error("❌ Anomaly insert error:", e.message);
      }
    },
  });
})();

// Report Consumer: Write to DB
(async () => {
  await reportConsumer.connect();
  await reportConsumer.subscribe({ topic: "reports", fromBeginning: false });

  await reportConsumer.run({
  eachMessage: async ({ message }) => {
    try {
      const key = message.key?.toString();
      const value = message.value?.toString();

      if (key === "__RESET__" && value === "__RESET__") {
        console.log("🔄 Reset signal received — clearing reports collection");
        await Report.deleteMany({});
      } else {
        await Report.create({ value });
      }
    } catch (e) {
      console.error("❌ Report insert error:", e.message);
    }
  },
});
})();

setInterval(async () => {
  try {
    const trades = await Trade.find().sort({ _id: -1 }).limit(5);
    const anomaly = await Anomaly.findOne().sort({ _id: -1 });

    // Fetch latest summary messages from MongoDB
    const summaries = await Report.find().sort({ _id: -1 }).limit(20);

    io.emit("liveTrade", trades.map(t => ({
      symbol: t.symbol || t.stockSymbol,
      price: Number(t.price).toFixed(2),
      volume: t.volume,
    })));

    if (anomaly && anomaly.symbol && anomaly.percentageChange) {
      anomaly.message = `Anomaly Detected: ${anomaly.symbol} price changed by ${Number(anomaly.percentageChange).toFixed(2)}%`;
    }

    io.emit("anomalyDetected", anomaly?.message ? anomaly : {});
    io.emit("summaryUpdate", summaries.map(r => r.value)); // Send only value field
    const chartData = summaries.map(doc => {
  const [symbol, volumeStr] = doc.value.split(" = ");
  return {
    label: symbol.replace(": Total Volume", ""),
    value: parseInt(volumeStr)
  };
});
io.emit("chartUpdate", chartData);
  } catch (err) {
    console.error("❌ MongoDB read error:", err.message);
  }
}, 1000);

// WebSocket
io.on("connection", (socket) => {
  console.log("⚡ Web client connected");
  socket.on("disconnect", () => console.log("❌ Web client disconnected"));
});

server.listen(3001, () => {
  console.log("WebSocket server running on http://localhost:3001");
});
