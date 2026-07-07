import { useEffect, useState } from 'react';
import api, { getErrorMessage } from '../services/api';
import { FIAT_CURRENCIES, CRYPTO_CURRENCIES, TRADE_CURRENCIES } from '../constants/currencies';
import { formatNumber } from '../utils/format';

const POPULAR_PAIRS = [
  { from: 'EUR', to: 'RSD', kind: 'fiat' },
  { from: 'USD', to: 'RSD', kind: 'fiat' },
  { from: 'BTC', to: 'USD', kind: 'crypto' },
  { from: 'ETH', to: 'USD', kind: 'crypto' },
  { from: 'USD', to: 'EUR', kind: 'fiat' },
];

function FiatRateForm() {
  const [from, setFrom] = useState('EUR');
  const [to, setTo] = useState('RSD');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setResult(null);
    try {
      const response = await api.get('/currency-exchange', { params: { from, to } });
      setResult(response.data);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }

  return (
    <div className="card card-lg">
      <h3>Currency Exchange Rates</h3>
      <form className="form-inline" onSubmit={handleSubmit}>
        <div className="form-group">
          <label>From</label>
          <select
            className="input"
            value={from}
            onChange={(e) => {
              setError('');
              setResult(null);
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
              setResult(null);
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
        <div className="form-inline-action">
          <label className="spacer-label">Action</label>
          <button className="btn btn-primary" type="submit">
            Check rate
          </button>
        </div>
      </form>
      {result && (
        <p className="result-box">
          1 {result.fromCurrency} ={' '}
          <span className="result-value">
            {formatNumber(result.rate)} {result.toCurrencyCode}
          </span>
        </p>
      )}
      {error && <p className="alert alert-error">{String(error)}</p>}
    </div>
  );
}

function CryptoRateForm() {
  const [from, setFrom] = useState('BTC');
  const [to, setTo] = useState('USD');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setResult(null);
    try {
      const response = await api.get('/crypto-exchange', { params: { from, to } });
      setResult(response.data);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }

  return (
    <div className="card card-lg">
      <h3>Crypto Exchange Rates</h3>
      <form className="form-inline" onSubmit={handleSubmit}>
        <div className="form-group">
          <label>From</label>
          <select
            className="input"
            value={from}
            onChange={(e) => {
              setError('');
              setResult(null);
              setFrom(e.target.value);
            }}
          >
            {CRYPTO_CURRENCIES.map((c) => (
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
              setResult(null);
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
        <div className="form-inline-action">
          <label className="spacer-label">Action</label>
          <button className="btn btn-primary" type="submit">
            Check rate
          </button>
        </div>
      </form>
      {result && (
        <p className="result-box">
          1 {result.fromCurrency} ={' '}
          <span className="result-value">
            {formatNumber(result.rate)} {result.toCurrency}
          </span>
        </p>
      )}
      {error && <p className="alert alert-error">{String(error)}</p>}
    </div>
  );
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function fetchRate(pair) {
  const endpoint = pair.kind === 'fiat' ? '/currency-exchange' : '/crypto-exchange';
  const res = await api.get(endpoint, { params: { from: pair.from, to: pair.to } });
  return res.data.rate;
}

// Rate providers (esp. CoinGecko) occasionally 429/502 on transient rate limits,
// so each pair gets one retry after a short delay before giving up.
async function fetchRateWithRetry(pair) {
  try {
    return await fetchRate(pair);
  } catch {
    await sleep(2000);
    try {
      return await fetchRate(pair);
    } catch {
      return null;
    }
  }
}

function PopularConversions() {
  const [rates, setRates] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function loadAll() {
      const results = await Promise.all(
        POPULAR_PAIRS.map(async (pair) => ({ ...pair, rate: await fetchRateWithRetry(pair) }))
      );
      if (!cancelled) {
        setRates(results);
        setLoading(false);
      }
    }

    loadAll();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="card card-lg">
      <h3>Popular Conversions</h3>
      {loading && <p className="muted">Loading rates...</p>}
      {!loading && (
        <table className="table">
          <thead>
            <tr>
              <th>Pair</th>
              <th>Rate</th>
            </tr>
          </thead>
          <tbody>
            {rates.map((r) => (
              <tr key={`${r.from}-${r.to}`}>
                <td>
                  {r.from} &rarr; {r.to}
                </td>
                <td>
                  {r.rate !== null ? `1 ${r.from} = ${formatNumber(r.rate)} ${r.to}` : 'Unavailable'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default function ExchangeRatesPage() {
  return (
    <div>
      <div className="page-header">
        <h2>Exchange Rates</h2>
      </div>
      <div className="two-col-grid">
        <FiatRateForm />
        <CryptoRateForm />
      </div>
      <PopularConversions />
    </div>
  );
}
