# 📈 StockPulse – Real-Time Stock Trade Monitoring System
StockPulse is a real-time stock monitoring system that simulates high-frequency trading, detects anomalies, and streams live analytics to a responsive dashboard. Built with Kafka, MongoDB, Node.js, WebSockets, and Chart.js — it's designed to emulate fault-tolerant, production-grade fintech pipelines.

---

## 🚀 Features
- ⚡️ **High-Throughput Simulation**: 1000 trades generated in 20 seconds using a custom Java-based `TradeLoadSimulator`.
- 📦 **Kafka-Based Event Streaming**: Dockerized Apache Kafka with 4 producer partitions for high availability and throughput.
- 🧠 **Modular Consumers**:
  - `DashboardConsumer`: Streams real-time trades
  - `AnomalyConsumer`: Detects and stores outlier trades
  - `ReportConsumer`: Summarizes top 5 stock insights every 30 seconds
- 🗃️ **MongoDB Integration**: All processed data is stored in MongoDB (acts as a buffer and persistence layer).
- 🌐 **WebSocket Server**: Node.js server streams MongoDB data to connected frontend clients instantly.
- 📊 **Live Dashboard (Frontend)**:
  - Displays real-time trades
  - Highlights anomalies
  - Renders summaries 
  - Dynamic top-5 stock summary charts (Chart.js)

---

## 🧱 Architecture

![WhatsApp Image 2025-07-02 at 17 56 33_532d5773](https://github.com/user-attachments/assets/e80f0c04-3cb3-4e42-b8e8-866e08c81fd7)

---

## ⚙️ Technologies Used
| Stack               | Tools / Frameworks                       |
|---------------------|------------------------------------------|
| Language            | Java, JavaScript, Node.js                |
| Messaging System    | Apache Kafka (Docker)                    |
| Database            | MongoDB                                  |
| Frontend            | HTML, CSS, JavaScript, Chart.js          |
| Real-Time Protocol  | WebSocket (Node.js-based)                |
| Deployment Ready    | Docker, Docker Compose (Kafka setup)     |

---

## 📦 Kafka Partitioning Strategy
- Producer: 4 partitions
- Consumers: Each with 2 partitions
- Benefit: High availability + fault tolerance across consumer groups

---

## 🧪 Performance Testing
- 💥 `TradeLoadSimulator`: Generates 1000+ trades in under 20 seconds
- ⚙️ Handles real-time trade bursts with zero consumer lag under normal conditions

---

## 📌 Key Functional Highlights
- Detects sudden price/volume spikes in real-time (anomalies)
- Calculates top 5 traded stocks every 30 seconds (reporting)
- Sends live updates via WebSocket without page reloads

---
## 🛠️ Installation & Setup
Prerequisites:
Java ,Maven ,Node.js ,Docker & Docker Compose
1️⃣ Clone the Repository
git clone https://github.com/your-username/stockpulse.git
cd stockpulse
2️⃣ Start Kafka, Zookeeper & MongoDB (via Docker Compose)
docker-compose up -d
3️⃣ Run the Kafka Trade Producer
mvn clean compile
mvn exec:java -Dexec.mainClass=com.example.stockmonitor.producer.TradeProducer
4️⃣ Run the Load/Stress Simulator (1000 trades in 20 sec)
mvn exec:java -Dexec.mainClass=com.example.stockmonitor.producer.TradeLoadSimulator
5️⃣ Run the Kafka Consumers
Open 3 terminals or use IntelliJ multi-run
# Anomaly Consumer
mvn exec:java -Dexec.mainClass=com.example.stockmonitor.consumer.AnomalyConsumer
# Dashboard Consumer
mvn exec:java -Dexec.mainClass=com.example.stockmonitor.consumer.DashboardConsumer
# Report Consumer
mvn exec:java -Dexec.mainClass=com.example.stockmonitor.consumer.ReportConsumer
6️⃣ Start the WebSocket Server
cd web-dashboard
npm install
node server.js

---

## 📁 Directory structure:
└── gunjana-anitha-jain-stockpulse/
    ├── README.md
    ├── package.json
    ├── pom.xml
    ├── src/
    │   ├── main/
    │   │   └── java/
    │   │       └── com/
    │   │           └── example/
    │   │               └── stockmonitor/
    │   │                   ├── App.java
    │   │                   ├── consumer/
    │   │                   │   ├── AnomalyConsumer.java
    │   │                   │   ├── DashboardConsumer.java
    │   │                   │   ├── MetricsConsumer.java
    │   │                   │   └── ReportConsumer.java
    │   │                   ├── model/
    │   │                   │   └── Trade.java
    │   │                   └── producer/
    │   │                       ├── TradeLoadSimulator.java
    │   │                       └── TradeProducer.java
    │   └── test/
    │       └── java/
    │           └── com/
    │               └── example/
    │                   └── stockmonitor/
    │                       └── AppTest.java
    └── web-dashboard/
        ├── package.json
        ├── server.js
        └── public/
            ├── app.js
            ├── index.html
            └── style.css
            
---

## 🧠 System Design Principles
- **Decoupling**: Kafka acts as a buffer to decouple real-time data generation and consumption
- **Scalability**: Easily extendable by adding more Kafka partitions and consumer threads
- **Fault Tolerance**: Docker-based deployment + multi-partition design
- **Low Latency**: WebSocket protocol ensures fast push-based updates to clients
---

## 🔮 Future Enhancements
- [ ] Add REST API for historical data querying
- [ ] Swing-based Java dashboard (in progress)
- [ ] TTL indexes in MongoDB for automatic log cleanup
---

## 💡 Inspiration
This project was built to simulate and understand real-world event streaming, fault-tolerant design, and scalable system architecture — core principles behind platforms like stock exchanges, fintech dashboards, and e-commerce recommendation engines.

---
