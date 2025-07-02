const socket = io("http://localhost:3001");

    socket.on("connect", () => console.log("✅ Connected to WebSocket server"));
    socket.on("disconnect", () => console.log("❌ Disconnected from WebSocket server"));

    socket.on("liveTrade", (data) => {
      document.getElementById("liveTrades").textContent =
        data.map(t => `${t.symbol}: $${t.price} (${t.volume} shares)`).join("\n");
    });

    socket.on("anomalyDetected", (data) => {
      document.getElementById("anomalies").textContent = data.message || "No anomaly detected.";
    });

    socket.on("summaryUpdate", (summaries) => {
      const div = document.getElementById("summary");
      div.textContent = summaries.length === 0 ? "No summary available" : summaries.join("\n");
    });

    // Chart.js integration
    let chart;
    const ctx = document.getElementById("summaryChart").getContext("2d");

    socket.on("chartUpdate", (data) => {
      if (chart) chart.destroy();
      chart = new Chart(ctx, {
        type: "bar",
        data: {
          labels: data.map(d => d.label),
          datasets: [{
            label: "Total Volume",
            data: data.map(d => d.value),
            backgroundColor: "rgba(75, 192, 192, 0.6)",
            borderRadius: 5
          }]
        },
        options: {
          animation:false,
          responsive: true,
          plugins: {
            legend: { display: false },
            title: { display: true, text: '📈 Real-Time Volume (Top 5)' }
          },
          scales: {
            y: {
              beginAtZero: true,
              ticks: { precision: 0 }
            }
          }
        }
      });
    });