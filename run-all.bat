@echo off
echo ==========================================================
echo             STARTING STOCKPULSE ECOSYSTEM
echo ==========================================================

REM --- Step 1: (Optional) Launch Docker Containers ---
REM docker-compose up -d

echo [2/7] Compiling Java Modules...
call mvn clean compile -q

echo [3/7] Running Consumers (Dashboard, Anomaly, Report)...
start "TradeDisplay" cmd /c mvn exec:java -Dexec.mainClass=com.example.stockmonitor.consumer.TradeDisplay
start "DashboardConsumer" cmd /c mvn exec:java -Dexec.mainClass=com.example.stockmonitor.consumer.DashboardConsumer
start "AnomalyConsumer" cmd /c mvn exec:java -Dexec.mainClass=com.example.stockmonitor.consumer.AnomalyConsumer
start "ReportConsumer" cmd /c mvn exec:java -Dexec.mainClass=com.example.stockmonitor.consumer.ReportConsumer

echo [4/7] Running OrderBookEngine (simulated orders)...
start "OrderBookEngine" cmd /c mvn exec:java -Dexec.mainClass=com.example.stockmonitor.producer.OrderBookEngine

echo [7/7] Launching Web Dashboard...
cd web-dashboard
start "Dashboard" cmd /c npm start
cd ..

echo ==========================================================
echo ✅ StockPulse ecosystem is up and running!
echo 🌐 Open your dashboard at: http://localhost:3001
echo ==========================================================
pause
