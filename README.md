# 📈 StockPulse — Real-Time Trade Monitoring System

A distributed real-time market simulation system built using **Java, Kafka, WebSockets, and Node.js**.
Designed to emulate event-driven financial systems used in trading platforms (Coinbase, Robinhood, Zerodha) with:
✅ Live order-book engine (multi-stock)  
✅ Real-time matching & trade execution  
✅ Kafka-based streaming pipeline  
✅ Price anomaly detection  
✅ Top-5 traded volume insights  
✅ Real-time dashboard (WebSockets + Chart.js)

---

## 🏗 Architecture Diagram

<img width="764" height="481" alt="image" src="https://github.com/user-attachments/assets/460f3716-e01b-4ab2-bea6-50c4f38ef3a3" />
                
---

## 🧠 Core Features

### 📊 Order Book Engine (Java)
- Multi-asset trading (AAPL, TSLA, MSFT, AMZN, GOOG)
- Priority matching:
  - Max-heap bids
  - Min-heap asks
- Live order flow + execution + depth feed

### 🧵 Streaming & Pipelines
- Kafka topics:
  - `executed-trades`
  - `orderbook`
  - `anomalies`
  - `reports`
- Consumer groups auto-rebalance

### ⚠ Anomaly Detection
- Identifies abnormal price deviation (>10%)
- Streams alerts live to UI

### 🏆 Top-5 Trade Volume Insights
- Sliding window volume aggregation
- Chart.js bar graph visualization

### 🖥 Real-Time Dashboard
- Live trades feed
- Order book snapshot
- Market depth bars
- Anomaly ticker
- Volume leader leaderboard

---

## 🧰 Technology Stack

| Layer        | Tech                                             |
| ------------ | ------------------------------------------------ |
| Streaming    | Apache Kafka                                     |
| Backend      | Java 17, Jackson, Kafka Clients                  |
| Dashboard    | Node.js, Socket.IO                               |
| UI           | HTML, CSS, Chart.js                              |
| Architecture | Event-Driven Microstreaming                      |
| Deployment   | Render (Web), Kafka local / Confluent Cloud next |


---

## 📂 Project Structure

Directory structure:
└── gunjana-anitha-jain-stockpulse/
    ├── README.md
    ├── package.json
    ├── pom.xml
    ├── render.yaml
    ├── requirements.txt
    ├── run-all.bat
    ├── stop-stockpulse.bat
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
    │   │                   │   ├── ReportConsumer.java
    │   │                   │   └── TradeDisplay.java
    │   │                   ├── model/
    │   │                   │   ├── Order.java
    │   │                   │   └── Trade.java
    │   │                   └── producer/
    │   │                       ├── OrderBookEngine.java
    │   │                       └── TradeLoadSimulator.java
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

## How to Run (Local Dev)
1️⃣ Start Kafka
zookeeper-server-start.sh config/zookeeper.properties
kafka-server-start.sh config/server.properties

2️⃣ Run Engines
run-all.bat

3️⃣ Open Dashboard
http://localhost:3001

---

## 📸 UI Overview
| Module           | Function                       |
| ---------------- | ------------------------------ |
| Live Trades      | streaming feed                 |
| Order Book       | top-of-book + depth bars       |
| Volume Chart     | top-5 symbols by traded volume |
| Anomaly Feed     | real-time alerts               |
| Latency-aware UI | throttled updates              |

---

## ⚙️ Performance Notes
| Metric           | Value                        |
| ---------------- | ---------------------------- |
| Event throughput | ~1200 orders/min             |
| Latency          | ~20–40 ms                    |
| Scalability      | Consumer groups + partitions |
| Resilience       | Auto-reconnect + backoff     |

---

## 📚 Key Concepts Demonstrated:
✅Event-driven micro-pipelines
✅Kafka streaming & consumer groups
✅Order book & matching logic
✅Live depth visualization
✅Real-time WebSocket broadcasting
✅Clean async UI streaming logic

---

## ⭐ Outcome:
>>This is not a toy project.
>>It is a real-time distributed system with streaming, analytics, state, throttling, and concurrency control.
