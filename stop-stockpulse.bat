@echo off
echo ==========================================================
echo              STOPPING STOCKPULSE ECOSYSTEM
echo ==========================================================

echo [1/6] Stopping Java Modules...
taskkill /FI "WINDOWTITLE eq TradeDisplay*" /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq DashboardConsumer*" /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq AnomalyConsumer*" /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq ReportConsumer*" /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq OrderBookEngine*" /F >nul 2>&1

echo [2/6] Stopping Web Dashboard...
taskkill /FI "WINDOWTITLE eq Dashboard*" /F >nul 2>&1
taskkill /IM node.exe /F >nul 2>&1

echo [3/6] Optionally stopping Docker containers...
REM Uncomment below if using Docker for Kafka, MongoDB, etc.
REM docker-compose down -v

echo [4/6] Cleaning up orphaned terminals...
for %%N in (TradeDisplay DashboardConsumer AnomalyConsumer ReportConsumer OrderBookEngine Dashboard) do (
    taskkill /IM cmd.exe /FI "WINDOWTITLE eq %%N*" /F >nul 2>&1
)

echo [5/6] All StockPulse services stopped successfully.
echo ==========================================================
echo ✅ Ecosystem shut down cleanly.
echo ==========================================================
pause
