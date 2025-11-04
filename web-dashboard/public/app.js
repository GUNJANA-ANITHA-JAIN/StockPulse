const socket = io();
const liveTradesEl = document.getElementById('liveTrades');
const liveTradeCountEl = document.getElementById('liveTradeCount');
const anomaliesEl = document.getElementById('anomalies');
const orderbookTableEl = document.getElementById('orderbookTable');
const summaryEl = document.getElementById('summary');

let liveTrades = [];

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

// New: Receive anomalies and update UI
socket.on('anomalyDetected', (anomaly) => {
  const div = document.createElement('div');
  div.textContent = `⚠️ Anomaly detected: ${JSON.stringify(anomaly)}`;
  anomaliesEl.appendChild(div);
});

// New: Receive trade summaries and update UI
socket.on('tradeSummaries', (summary) => {
  if (!summary || Object.keys(summary).length === 0) {
    summaryEl.textContent = 'Waiting for summaries...';
    return;
  }
  summaryEl.textContent = JSON.stringify(summary, null, 2);
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

// Basic orderbook table update
function updateOrderbook(orderbook) {
  if (!orderbook) return;
  while (orderbookTableEl.rows.length > 1) {
    orderbookTableEl.deleteRow(1);
  }

  const bids = orderbook.bids || [];
  const asks = orderbook.asks || [];
  const maxRows = Math.max(bids.length, asks.length);

  for (let i = 0; i < maxRows; i++) {
    const row = orderbookTableEl.insertRow();

    const bidPriceCell = row.insertCell(0);
    const bidVolumeCell = row.insertCell(1);
    const askPriceCell = row.insertCell(2);
    const askVolumeCell = row.insertCell(3);

    if (bids[i]) {
      bidPriceCell.innerText = bids[i].price.toFixed(2);
      bidVolumeCell.innerText = bids[i].volume;
    } else {
      bidPriceCell.innerText = '';
      bidVolumeCell.innerText = '';
    }

    if (asks[i]) {
      askPriceCell.innerText = asks[i].price.toFixed(2);
      askVolumeCell.innerText = asks[i].volume;
    } else {
      askPriceCell.innerText = '';
      askVolumeCell.innerText = '';
    }
  }
}
