import { useEffect, useState } from 'react';
import api, { getErrorMessage } from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function BankAccounts({ mode }) {
  const { user } = useAuth();
  const [accounts, setAccounts] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const [editingKey, setEditingKey] = useState(null); // `${email}|${currencyCode}`
  const [formEmail, setFormEmail] = useState('');
  const [formCurrency, setFormCurrency] = useState('');
  const [formAmount, setFormAmount] = useState('');

  async function loadAccounts() {
    setLoading(true);
    setError('');
    try {
      if (mode === 'admin') {
        const res = await api.get('/accounts');
        setAccounts(res.data);
      } else {
        const res = await api.get('/accounts/email', { params: { email: user.email } });
        setAccounts(res.data);
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAccounts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function resetForm() {
    setEditingKey(null);
    setFormEmail('');
    setFormCurrency('');
    setFormAmount('');
  }

  function startEdit(a) {
    setEditingKey(`${a.email}|${a.currencyCode}`);
    setFormEmail(a.email);
    setFormCurrency(a.currencyCode);
    setFormAmount(a.amount);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      const body = { email: formEmail, currencyCode: formCurrency, amount: Number(formAmount) };
      if (editingKey) {
        await api.put('/accounts', body);
      } else {
        await api.post('/accounts', body);
      }
      resetForm();
      await loadAccounts();
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }

  async function handleDelete(email, currencyCode) {
    setError('');
    try {
      await api.delete('/accounts', { params: { email, currencyCode } });
      await loadAccounts();
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }

  return (
    <div>
      <h3>{mode === 'admin' ? 'Bank accounts (all)' : 'My bank account'}</h3>

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
          {accounts.map((a) => (
            <tr key={`${a.email}|${a.currencyCode}`}>
              <td>{a.email}</td>
              <td>{a.currencyCode}</td>
              <td>{a.amount}</td>
              {mode === 'admin' && (
                <td>
                  <button onClick={() => startEdit(a)}>Edit</button>
                  <button onClick={() => handleDelete(a.email, a.currencyCode)}>Delete</button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>

      {mode === 'admin' && (
        <>
          <h4>{editingKey ? `Editing account: ${editingKey}` : 'Add account'}</h4>
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
              <label>Currency:</label>
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
            <button type="submit">{editingKey ? 'Save changes' : 'Add account'}</button>
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
