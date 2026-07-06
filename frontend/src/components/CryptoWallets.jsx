import { useEffect, useState } from 'react';
import api, { getErrorMessage } from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function CryptoWallets({ mode }) {
  const { user } = useAuth();
  const [wallets, setWallets] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const [editingKey, setEditingKey] = useState(null); // `${email}|${cryptoCurrencyCode}`
  const [formEmail, setFormEmail] = useState('');
  const [formCurrency, setFormCurrency] = useState('');
  const [formAmount, setFormAmount] = useState('');

  async function loadWallets() {
    setLoading(true);
    setError('');
    try {
      if (mode === 'admin') {
        const res = await api.get('/wallets');
        setWallets(res.data);
      } else {
        const res = await api.get('/wallets/email', { params: { email: user.email } });
        setWallets(res.data);
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadWallets();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function resetForm() {
    setEditingKey(null);
    setFormEmail('');
    setFormCurrency('');
    setFormAmount('');
  }

  function startEdit(w) {
    setEditingKey(`${w.email}|${w.cryptoCurrencyCode}`);
    setFormEmail(w.email);
    setFormCurrency(w.cryptoCurrencyCode);
    setFormAmount(w.amount);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      const body = { email: formEmail, cryptoCurrencyCode: formCurrency, amount: Number(formAmount) };
      if (editingKey) {
        await api.put('/wallets', body);
      } else {
        await api.post('/wallets', body);
      }
      resetForm();
      await loadWallets();
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }

  async function handleDelete(email, cryptoCurrencyCode) {
    setError('');
    try {
      await api.delete('/wallets', { params: { email, cryptoCurrencyCode } });
      await loadWallets();
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }

  return (
    <div>
      <h3>{mode === 'admin' ? 'Crypto wallets (all)' : 'My crypto wallet'}</h3>

      {loading && <p>Loading...</p>}
      {error && <p style={{ color: 'red' }}>{error}</p>}

      <table border="1" cellPadding="4">
        <thead>
          <tr>
            <th>Email</th>
            <th>Currency</th>
            <th>Balance</th>
            {mode === 'admin' && <th>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {wallets.map((w) => (
            <tr key={`${w.email}|${w.cryptoCurrencyCode}`}>
              <td>{w.email}</td>
              <td>{w.cryptoCurrencyCode}</td>
              <td>{w.amount}</td>
              {mode === 'admin' && (
                <td>
                  <button onClick={() => startEdit(w)}>Edit</button>
                  <button onClick={() => handleDelete(w.email, w.cryptoCurrencyCode)}>Delete</button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>

      {mode === 'admin' && (
        <>
          <h4>{editingKey ? `Editing wallet: ${editingKey}` : 'Add wallet'}</h4>
          <form onSubmit={handleSubmit}>
            <div>
              <label>Email:</label>
              <input
                type="email"
                value={formEmail}
                onChange={(e) => setFormEmail(e.target.value)}
                disabled={!!editingKey}
                required
              />
            </div>
            <div>
              <label>Crypto currency:</label>
              <input
                type="text"
                value={formCurrency}
                onChange={(e) => setFormCurrency(e.target.value)}
                disabled={!!editingKey}
                required
              />
            </div>
            <div>
              <label>Amount:</label>
              <input
                type="number"
                step="any"
                value={formAmount}
                onChange={(e) => setFormAmount(e.target.value)}
                required
              />
            </div>
            <button type="submit">{editingKey ? 'Save changes' : 'Add wallet'}</button>
            {editingKey && (
              <button type="button" onClick={resetForm}>
                Cancel
              </button>
            )}
          </form>
        </>
      )}
    </div>
  );
}
