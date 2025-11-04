const socket = io();
const liveTradesEl = document.getElementById('liveTrades');
const liveTradeCountEl = document.getElementById('liveTradeCount');
const anomaliesEl = document.getElementById('anomalies');
const orderbookTableEl = document.getElementById('orderbookTable');
const summaryEl = document.getElementById('summary');
// const obSymbol = document.getElementById('obSymbol');
let liveTrades = [];
let summaryChart;
const obSymbol = document.getElementById("ob-meta");   // reusing for symbol/time
const depthBids = document.getElementById("depthBids");
const depthAsks = document.getElementById("depthAsks");

socket.on('connect', () => {
  console.log('Connected to WebSocket');
});

socket.on('disconnect', () => {
  console.log('Disconnected from WebSocket');
});

// Receive real-time trades
socket.on('liveTrade', (trade) => {
  liveTrades.push(trade);
  if (liveTrades.length > 50) liveTrades.shift();
  updateTradeUI();
});

socket.on('liveTradesSnapshot', (trades) => {
  liveTrades = trades || [];
  updateTradeUI();
});

socket.on('orderbookUpdate', (orderbook) => {
  updateOrderbook(orderbook);
});

// // New: Receive anomalies and update UI
// socket.on('anomalyDetected', (anomaly) => {
//   const div = document.createElement('div');
//   div.textContent = `⚠️ Anomaly detected: ${JSON.stringify(anomaly)}`;
//   anomaliesEl.appendChild(div);
// });

// // New: Receive trade summaries and update UI
// socket.on('tradeSummaries', (summary) => {
//   if (summary.reset) {
//     summaryEl.textContent = "Waiting for summaries...";
//     return;
//   }

//   if (!summary.top5) return;

//   summaryEl.textContent = summary.top5
//     .map(s => `${s.stock}: ${s.volume}`)
//     .join("\n");
// });
// --- replace your anomaly + summary handlers with these ---

socket.on('anomalyDetected', (anomaly) => {
  if (anomaliesEl.innerText.trim() === "No anomalies yet") anomaliesEl.innerText = "";
  const div = document.createElement('div');
  div.textContent = `⚠️ ${anomaly.stockSymbol || ''} ${anomaly.message || ''}`.trim();
  anomaliesEl.prepend(div);
  while (anomaliesEl.childElementCount > 5) anomaliesEl.removeChild(anomaliesEl.lastChild);
});

socket.on('tradeSummaries', (summary) => {
  if (summary && summary.reset) {
    summaryEl.textContent = "Waiting for summaries...";
    if (summaryChart) {
      summaryChart.destroy();
      summaryChart = null;
    }
    return;
  }

  if (!summary || !summary.top5) return;

  const labels = summary.top5.map(s => s.stock);
  const volumes = summary.top5.map(s => s.volume);

  summaryEl.textContent = summary.top5.map(s => `${s.stock}: ${s.volume}`).join("\n");

  const ctx = document.getElementById("summaryChart").getContext('2d');

  if (summaryChart) summaryChart.destroy();

  summaryChart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels,
      datasets: [{
        label: 'Top 5 Traded Stocks',
        data: volumes
      }]
    },
    options: {
      plugins: { legend: { display: false } },
      animation: false
    }
  });
});


// Debounced DOM update for trades
let tradeUpdateTimeout;
function updateTradeUI() {
  if (tradeUpdateTimeout) clearTimeout(tradeUpdateTimeout);
  tradeUpdateTimeout = setTimeout(() => {
    liveTradesEl.innerHTML = liveTrades.slice(-15).map(t =>
      `<div>${t.stockSymbol} @ ${Number(t.price).toFixed(2)} vol: ${t.volume}</div>`
    ).join('');
    liveTradeCountEl.innerText = `Trades: ${liveTrades.length}`;
  }, 100);
}


function updateOrderbook(orderbook) {
  if (!orderbook || !orderbook.bids || !orderbook.asks) return;

  const tbody = document
    .getElementById("orderbookTable")
    .querySelector("tbody");
  tbody.innerHTML = "";

  // Header meta
  const time = new Date().toLocaleTimeString();
  obSymbol.textContent = `Symbol: ${orderbook.symbol || "-"} | Time: ${time}`;

  const bids = orderbook.bids.slice(0, 5); // ensure 5 levels
  const asks = orderbook.asks.slice(0, 5);

  const rows = Math.max(bids.length, asks.length);

  // Determine previous best (to flash on change)
  let prevBestBid = tbody.getAttribute("data-prev-best-bid");
  let prevBestAsk = tbody.getAttribute("data-prev-best-ask");
  const bestBid = bids[0]?.price ?? null;
  const bestAsk = asks[0]?.price ?? null;

  for (let i = 0; i < rows; i++) {
    const bid = bids[i] || {};
    const ask = asks[i] || {};

    const tr = document.createElement("tr");

    // best levels get 'best' class; also flash if changed
    const isBestBid = i === 0 && bid.price != null;
    const isBestAsk = i === 0 && ask.price != null;

    const bidPriceCell = document.createElement("td");
    const bidVolCell   = document.createElement("td");
    const askPriceCell = document.createElement("td");
    const askVolCell   = document.createElement("td");

    bidPriceCell.className = "bid-t" + (isBestBid ? " best" : "");
    bidVolCell.className   = "bid-t" + (isBestBid ? " best" : "");
    askPriceCell.className = "ask-t" + (isBestAsk ? " best" : "");
    askVolCell.className   = "ask-t" + (isBestAsk ? " best" : "");

    bidPriceCell.textContent = (bid.price != null && bid.price.toFixed) ? bid.price.toFixed(2) : "";
    bidVolCell.textContent   = bid.volume ?? "";
    askPriceCell.textContent = (ask.price != null && ask.price.toFixed) ? ask.price.toFixed(2) : "";
    askVolCell.textContent   = ask.volume ?? "";

    // flash if best changed
    if (isBestBid && prevBestBid && Number(prevBestBid) !== Number(bid.price)) {
      bidPriceCell.classList.add("flash");
      bidVolCell.classList.add("flash");
      setTimeout(() => { bidPriceCell.classList.remove("flash"); bidVolCell.classList.remove("flash"); }, 520);
    }
    if (isBestAsk && prevBestAsk && Number(prevBestAsk) !== Number(ask.price)) {
      askPriceCell.classList.add("flash");
      askVolCell.classList.add("flash");
      setTimeout(() => { askPriceCell.classList.remove("flash"); askVolCell.classList.remove("flash"); }, 520);
    }

    tr.appendChild(bidPriceCell);
    tr.appendChild(bidVolCell);
    tr.appendChild(askPriceCell);
    tr.appendChild(askVolCell);
    tbody.appendChild(tr);
  }

  // store current bests
  if (bestBid != null) tbody.setAttribute("data-prev-best-bid", String(bestBid));
  if (bestAsk != null) tbody.setAttribute("data-prev-best-ask", String(bestAsk));

  // render vertical depth
  renderVerticalDepth(bids, asks);
}

/* Coinbase-like vertical translucent depth blocks (B vertical) */
function renderVerticalDepth(bids, asks) {
  depthBids.innerHTML = "";
  depthAsks.innerHTML = "";

  const maxVol = Math.max(
    ...bids.map(b => b.volume || 0),
    ...asks.map(a => a.volume || 0),
    1
  );

  // we’ll map volume -> block height (px) and opacity
  const totalSlots = 5;         // 5 levels each side
  const columnHeight = depthBids.clientHeight || 260; // fallback match CSS
  const gapPx = 4;
  const baseHeight = (columnHeight - gapPx * (totalSlots - 1)) / totalSlots; // uniform, Coinbase-like
  // (If you want proportional heights, set baseHeight by vol too.)

  // bids: stack from bottom
  for (let i = 0; i < bids.length; i++) {
    const v = bids[i].volume || 0;
    const block = document.createElement("div");
    block.className = "depth-block";
    block.style.height = `${baseHeight}px`;
    block.style.opacity = (0.25 + 0.75 * (v / maxVol)).toFixed(2);
    depthBids.appendChild(block);
  }

  // asks: stack from top
  for (let i = 0; i < asks.length; i++) {
    const v = asks[i].volume || 0;
    const block = document.createElement("div");
    block.className = "depth-block";
    block.style.height = `${baseHeight}px`;
    block.style.opacity = (0.25 + 0.75 * (v / maxVol)).toFixed(2);
    depthAsks.appendChild(block);
  }
}
