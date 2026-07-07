import { useEffect, useState } from 'react';
import api, { getErrorMessage } from '../services/api';
import { FIAT_CURRENCIES } from '../constants/currencies';
import { formatNumber } from '../utils/format';

export default function CurrencyConversion({ onSuccess, size }) {
  const [from, setFrom] = useState('EUR');
  const [to, setTo] = useState('RSD');
  const [quantity, setQuantity] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const [preview, setPreview] = useState(null);

  useEffect(() => {
    if (!from || !to || !quantity || Number(quantity) <= 0) {
      setPreview(null);
      return;
    }
    if (from === to) {
      setPreview(null);
      return;
    }

    let cancelled = false;
    const timeoutId = setTimeout(async () => {
      try {
        const res = await api.get('/currency-exchange', { params: { from, to } });
        if (!cancelled) {
          setPreview(Number(quantity) * res.data.rate);
        }
      } catch {
        if (!cancelled) {
          setPreview(null);
        }
      }
    }, 400);

    return () => {
      cancelled = true;
      clearTimeout(timeoutId);
    };
  }, [from, to, quantity]);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);
    try {
      const res = await api.get('/currency-conversion', { params: { from, to, quantity } });
      setMessage(res.data.message);
      setQuantity('');
      setPreview(null);
      onSuccess?.();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className={size === 'lg' ? 'card card-lg' : 'card'}>
      <h3>Currency Conversion</h3>
      <form className="form-inline" onSubmit={handleSubmit}>
        <div className="form-group">
          <label>From</label>
          <select
            className="input"
            value={from}
            onChange={(e) => {
              setError('');
              setMessage('');
              setFrom(e.target.value);
            }}
          >
            {FIAT_CURRENCIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </div>
        <div className="form-group">
          <label>To</label>
          <select
            className="input"
            value={to}
            onChange={(e) => {
              setError('');
              setMessage('');
              setTo(e.target.value);
            }}
          >
            {FIAT_CURRENCIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </div>
        <div className="form-group">
          <label>Quantity</label>
          <input
            className="input"
            type="number"
            step="any"
            value={quantity}
            onChange={(e) => {
              setError('');
              setMessage('');
              setQuantity(e.target.value);
            }}
            required
          />
        </div>
        <div className="form-inline-action">
          <label className="spacer-label">Action</label>
          <button className="btn btn-success" type="submit" disabled={loading}>
            Exchange
          </button>
        </div>
      </form>

      {preview !== null && (
        <p className="result-box">
          Estimated result: ~<span className="result-value">{formatNumber(preview)} {to}</span>
        </p>
      )}

      {message && <p className="alert alert-success">{message}</p>}
      {error && <p className="alert alert-error">{error}</p>}
    </div>
  );
}
