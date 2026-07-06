import { useState } from 'react';
import api, { getErrorMessage } from '../services/api';

export default function TradeService() {
  const [from, setFrom] = useState('BTC');
  const [to, setTo] = useState('ETH');
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
      const res = await api.get('/trade-service', { params: { from, to, quantity } });
      setResult(res.data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  const rows = result ? result.accounts || result.wallets || [] : [];
  const isWallets = result && Array.isArray(result.wallets);

  return (
    <div>
      <h3>Crypto/fiat exchange (trade-service)</h3>
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
              {rows.map((r) => (
                <tr key={isWallets ? r.cryptoCurrencyCode : r.currencyCode}>
                  <td>{isWallets ? r.cryptoCurrencyCode : r.currencyCode}</td>
                  <td>{r.amount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
