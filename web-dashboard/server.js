require('dotenv').config();

const express = require('express');
const http = require('http');
const { Kafka } = require('kafkajs');
const socketIo = require('socket.io');
const mongoose = require('mongoose');

const PORT = process.env.PORT || 3001;
const app = express();
const server = http.createServer(app);
const io = socketIo(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST']
  }
});

app.use(express.static('public'));

// MongoDB setup
mongoose.connect(process.env.MONGOURI, { useNewUrlParser: true, useUnifiedTopology: true })
  .then(() => console.log('MongoDB connected'))
  .catch(err => console.error('MongoDB error:', err.message));

const AnySchema = new mongoose.Schema({}, { strict: false });
const Trade = mongoose.model('Trade', AnySchema, 'trades');
const OB = mongoose.model('Orderbook', AnySchema, 'orderbook');

// Kafka setup
const kafka = new Kafka({
  clientId: 'stockpulse-dashboard',
  brokers: [process.env.KAFKABROKER || 'localhost:9092']
});

const consumer = kafka.consumer({ groupId: 'dashboard-' + Math.random().toString(36).slice(2, 8) });

let liveTrades = [];
let orderbooks = {};

async function startConsumers() {
  await consumer.connect();
  await consumer.subscribe({ topics: ['executed-trades', 'orderbook', 'anomalies', 'reports'], fromBeginning: false });

  await consumer.run({
  eachMessage: async ({ topic, message }) => {
    const rawValue = message.value.toString();
    let parsed;
    try {
      parsed = JSON.parse(rawValue);
    } catch (e) {
      console.warn(`Skipping non-JSON message on topic ${topic}:`, rawValue);
      return; // skip message processing, don't throw
    }

    switch (topic) {
      case 'executed-trades':
        liveTrades.push(parsed);
        if (liveTrades.length > 25) liveTrades.shift();
        io.emit('liveTrade', parsed);
        io.emit('liveTradesSnapshot', liveTrades.slice(-15));
        break;
      case 'orderbook':
        orderbooks = parsed;
        io.emit('orderbookUpdate', parsed);
        break;
      case 'anomalies':
        io.emit('anomalyDetected', parsed);
        break;
      case 'reports':
        io.emit('tradeSummaries', parsed);
        break;
    }
  }
});


  consumer.on(consumer.events.CRASH, async (event) => {
    console.error('Kafka consumer crashed:', event.payload.error);
    try {
      await consumer.disconnect();
      await consumer.connect();
      await consumer.subscribe({ topics: ['executed-trades', 'orderbook', 'anomalies', 'reports'], fromBeginning: false });
    } catch (reconnectError) {
      console.error('Error reconnecting Kafka consumer:', reconnectError);
    }
  });
}

io.on('connection', (socket) => {
  console.log('New client connected, socket id:', socket.id);
  // Send initial snapshots on connection
  socket.emit('liveTradesSnapshot', liveTrades.slice(-15));
  socket.emit('orderbookUpdate', orderbooks);

  socket.on('disconnect', () => {
    console.log('Client disconnected, socket id:', socket.id);
  });
});

startConsumers().catch(err => {
  console.error('Failed to start Kafka consumers:', err);
});

server.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
