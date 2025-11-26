import http from 'k6/http';
import { check } from 'k6';

// 1. Configuration for the load test
export const options = {
  vus: 50,           // 50 Virtual Users
  duration: '30s',   // Run for 30 seconds
};

// 2. The Test Logic (The "Main" function)
export default function () {

  // Generate random price fluctuations so the graph dances
  const payload = JSON.stringify([
    { symbol: "AAPL", price: 150.0 + (Math.random() * 10), timestamp: Date.now() },
    { symbol: "GOOGL", price: 2800.0 + (Math.random() * 50), timestamp: Date.now() },
    { symbol: "MSFT", price: 299.0 + (Math.random() * 5), timestamp: Date.now() },
    { symbol: "TSLA", price: 750.0 + (Math.random() * 20), timestamp: Date.now() }
  ]);

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  // Send the request
  const res = http.post('http://localhost:8080/api/v1/ingest', payload, params);

  // Check if the server accepted it (Status 202)
  check(res, {
    'is status 202': (r) => r.status === 202,
  });
}