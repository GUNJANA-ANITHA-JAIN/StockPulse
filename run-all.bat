@echo off
echo ==========================================================
echo             STARTING STOCKPULSE ECOSYSTEM
echo ==========================================================

REM ----------- COMPILE PROJECT -----------
echo [1/6] Compiling Java modules...
call mvn clean compile -q

REM ----------- START KAFKA CONSUMERS -----------
echo [2/6] Starting Kafka Consumers...

start "DashboardConsumer" cmd /k mvn exec:java -Dexec.mainClass=com.example.stockmonitor.consumer.DashboardConsumer
start "AnomalyConsumer" cmd /k mvn exec:java -Dexec.mainClass=com.example.stockmonitor.consumer.AnomalyConsumer
start "ReportConsumer" cmd /k mvn exec:java -Dexec.mainClass=com.example.stockmonitor.consumer.ReportConsumer

REM (Optional - TradeDisplay, if you use it)
start "TradeDisplay" cmd /k mvn exec:java -Dexec.mainClass=com.example.stockmonitor.consumer.TradeDisplay

REM ----------- START PRODUCERS -----------
echo [3/6] Starting OrderBook Engine (Java, real data)...
start "OrderBookEngine" cmd /k mvn exec:java -Dexec.mainClass=com.example.stockmonitor.producer.OrderBookEngine

@REM echo [4/6] Starting Trade Load Simulator...
@REM start "TradeSimulator" cmd /k mvn exec:java -Dexec.mainClass=com.example.stockmonitor.producer.TradeLoadSimulator

REM ----------- START WEB UI SERVER -----------
echo [5/6] Launching Web Dashboard...
cd web-dashboard
start "Dashboard Server" cmd /k npm start
cd ..

echo ==========================================================
echo ✅ All services started!
echo 🌐 Open Dashboard: http://localhost:3001
echo 📦 Kafka Topics: executed-trades | orderbook | anomalies | reports
echo ==========================================================

pause
