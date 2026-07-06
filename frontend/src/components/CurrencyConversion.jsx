import { useState } from 'react';
import api, { getErrorMessage } from '../services/api';

export default function CurrencyConversion() {
  const [from, setFrom] = useState('EUR');
  const [to, setTo] = useState('RSD');
  const [quantity, setQuantity] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setResult(null);
    setLoading(true);
    try {
      const res = await api.get('/currency-conversion', { params: { from, to, quantity } });
      setResult(res.data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <h3>Fiat currency exchange (currency-conversion)</h3>
      <form onSubmit={handleSubmit}>
        <label>From:</label>
        <input value={from} onChange={(e) => setFrom(e.target.value)} required />
        <label>To:</label>
        <input value={to} onChange={(e) => setTo(e.target.value)} required />
        <label>Quantity:</label>
        <input type="number" step="any" value={quantity} onChange={(e) => setQuantity(e.target.value)} required />
        <button type="submit" disabled={loading}>
          Exchange
        </button>
      </form>

      {error && <p style={{ color: 'red' }}>{error}</p>}

      {result && (
        <div>
          <p>{result.message}</p>
          <table border="1" cellPadding="4">
            <thead>
              <tr>
                <th>Currency</th>
                <th>Balance</th>
              </tr>
            </thead>
            <tbody>
              {result.accounts.map((a) => (
                <tr key={a.currencyCode}>
                  <td>{a.currencyCode}</td>
                  <td>{a.amount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
