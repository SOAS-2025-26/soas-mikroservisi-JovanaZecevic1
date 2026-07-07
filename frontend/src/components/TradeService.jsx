import { useEffect, useState } from 'react';
import api, { getErrorMessage } from '../services/api';
import { TRADE_CURRENCIES, CRYPTO_CURRENCIES } from '../constants/currencies';
import { formatNumber } from '../utils/format';

async function fetchCryptoRate(from, to) {
  const res = await api.get('/crypto-exchange', { params: { from, to } });
  return res.data.rate;
}

async function fetchFiatRate(from, to) {
  const res = await api.get('/currency-exchange', { params: { from, to } });
  return res.data.rate;
}

async function computePreview(from, to, quantity) {
  const fromCrypto = CRYPTO_CURRENCIES.includes(from);
  const toCrypto = CRYPTO_CURRENCIES.includes(to);

  if (fromCrypto && toCrypto) {
    const rate = await fetchCryptoRate(from, to);
    return quantity * rate;
  }

  if (fromCrypto && !toCrypto) {
    try {
      const rate = await fetchCryptoRate(from, to);
      return quantity * rate;
    } catch {
      const cryptoUsd = await fetchCryptoRate(from, 'USD');
      const usdAmount = quantity * cryptoUsd;
      if (to === 'USD') return usdAmount;
      const usdToTarget = await fetchFiatRate('USD', to);
      return usdAmount * usdToTarget;
    }
  }

  if (!fromCrypto && toCrypto) {
    const usdAmount = from === 'USD' ? quantity : quantity * (await fetchFiatRate(from, 'USD'));
    const cryptoUsd = await fetchCryptoRate(to, 'USD');
    return usdAmount / cryptoUsd;
  }

  return null;
}

export default function TradeService({ onSuccess, size }) {
  const [from, setFrom] = useState('BTC');
  const [to, setTo] = useState('ETH');
  const [quantity, setQuantity] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [preview, setPreview] = useState(null);

  useEffect(() => {
    if (!from || !to || from === to || !quantity || Number(quantity) <= 0) {
      setPreview(null);
      return;
    }

    let cancelled = false;
    const timeoutId = setTimeout(async () => {
      try {
        const estimate = await computePreview(from, to, Number(quantity));
        if (!cancelled) {
          setPreview(estimate);
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
      const res = await api.get('/trade-service', { params: { from, to, quantity } });
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
      <h3>Crypto Trading</h3>
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
            {TRADE_CURRENCIES.map((c) => (
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
            {TRADE_CURRENCIES.map((c) => (
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
          Approximately: ~<span className="result-value">{formatNumber(preview)} {to}</span>
        </p>
      )}

      {message && <p className="alert alert-success">{message}</p>}
      {error && <p className="alert alert-error">{error}</p>}
    </div>
  );
}
