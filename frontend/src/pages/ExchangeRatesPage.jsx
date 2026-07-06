import { useState } from 'react';
import api from '../services/api';

function RateForm({ title, endpoint, defaultFrom, defaultTo }) {
  const [from, setFrom] = useState(defaultFrom);
  const [to, setTo] = useState(defaultTo);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setResult(null);
    try {
      const response = await api.get(endpoint, { params: { from, to } });
      setResult(response.data);
    } catch (err) {
      setError(
        (err.response && (err.response.data?.message || err.response.data)) ||
          'Greška prilikom dobavljanja kursa.'
      );
    }
  }

  return (
    <div>
      <h3>{title}</h3>
      <form onSubmit={handleSubmit}>
        <label>Iz:</label>
        <input value={from} onChange={(e) => setFrom(e.target.value)} required />
        <label>U:</label>
        <input value={to} onChange={(e) => setTo(e.target.value)} required />
        <button type="submit">Proveri kurs</button>
      </form>
      {result && <pre>{JSON.stringify(result, null, 2)}</pre>}
      {error && <p style={{ color: 'red' }}>{String(error)}</p>}
    </div>
  );
}

export default function ExchangeRatesPage() {
  return (
    <div>
      <h2>Kursevi valuta</h2>
      <RateForm
        title="Fiat valute (currency-exchange)"
        endpoint="/currency-exchange"
        defaultFrom="EUR"
        defaultTo="RSD"
      />
      <RateForm
        title="Crypto valute (crypto-exchange)"
        endpoint="/crypto-exchange"
        defaultFrom="BTC"
        defaultTo="USD"
      />
    </div>
  );
}
