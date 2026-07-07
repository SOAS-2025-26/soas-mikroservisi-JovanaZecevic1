import { useEffect, useState } from 'react';
import api, { getErrorMessage } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { formatNumber, roundForInput } from '../utils/format';
import { CRYPTO_CURRENCIES } from '../constants/currencies';
import ConfirmDialog from './ConfirmDialog';

export default function CryptoWallets({ mode, refreshSignal, size }) {
  const { user } = useAuth();
  const [wallets, setWallets] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const [showAddForm, setShowAddForm] = useState(false);
  const [editingKey, setEditingKey] = useState(null);
  const [pendingDelete, setPendingDelete] = useState(null);
  const [formEmail, setFormEmail] = useState('');
  const [formCurrency, setFormCurrency] = useState(CRYPTO_CURRENCIES[0]);
  const [formAmount, setFormAmount] = useState('');

  const isFormOpen = showAddForm || !!editingKey;

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
  }, [refreshSignal]);

  function resetForm() {
    setError('');
    setShowAddForm(false);
    setEditingKey(null);
    setFormEmail('');
    setFormCurrency(CRYPTO_CURRENCIES[0]);
    setFormAmount('');
  }

  function startAdd() {
    setError('');
    setEditingKey(null);
    setFormEmail('');
    setFormCurrency(CRYPTO_CURRENCIES[0]);
    setFormAmount('');
    setShowAddForm(true);
  }

  function startEdit(w) {
    setError('');
    setShowAddForm(false);
    setEditingKey(`${w.email}|${w.cryptoCurrencyCode}`);
    setFormEmail(w.email);
    setFormCurrency(w.cryptoCurrencyCode);
    setFormAmount(roundForInput(w.amount));
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

  function confirmDelete() {
    const { email, cryptoCurrencyCode } = pendingDelete;
    setPendingDelete(null);
    handleDelete(email, cryptoCurrencyCode);
  }

  const groupedWallets =
    mode === 'admin'
      ? Object.entries(
          wallets.reduce((acc, w) => {
            (acc[w.email] ||= []).push(w);
            return acc;
          }, {})
        )
      : null;

  return (
    <div className={size === 'lg' ? 'card card-lg' : 'card'}>
      <h3 className="section-title">{mode === 'admin' ? 'Crypto wallets' : 'My Crypto Wallet'}</h3>

      {loading && <p className="muted">Loading...</p>}
      {error && <p className="alert alert-error">{error}</p>}

      {mode === 'admin' ? (
        groupedWallets.length === 0 ? (
          <p className="muted">No wallets yet.</p>
        ) : (
          groupedWallets.map(([email, rows]) => (
            <div className="grouped-table" key={email}>
              <h4 className="group-heading">{email}</h4>
              <table className="table">
                <thead>
                  <tr>
                    <th>Currency</th>
                    <th>Balance</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((w) => (
                    <tr key={`${w.email}|${w.cryptoCurrencyCode}`}>
                      <td>{w.cryptoCurrencyCode}</td>
                      <td>{formatNumber(w.amount)}</td>
                      <td>
                        <button className="btn btn-secondary" onClick={() => startEdit(w)}>
                          Edit
                        </button>
                        <button
                          className="btn btn-danger"
                          onClick={() => setPendingDelete({ email: w.email, cryptoCurrencyCode: w.cryptoCurrencyCode })}
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ))
        )
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Currency</th>
              <th>Balance</th>
            </tr>
          </thead>
          <tbody>
            {wallets.map((w) => (
              <tr key={`${w.email}|${w.cryptoCurrencyCode}`}>
                <td>{w.cryptoCurrencyCode}</td>
                <td>{formatNumber(w.amount)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {mode === 'admin' && !isFormOpen && (
        <div className="add-toggle-wrapper">
          <button className="btn btn-success" onClick={startAdd}>
            + Add wallet
          </button>
        </div>
      )}

      {mode === 'admin' && isFormOpen && (
        <div className="entity-form-wrapper">
          <div className={editingKey ? 'entity-form entity-form-editing' : 'entity-form'}>
            <h4 className={editingKey ? 'entity-form-title entity-form-title-editing' : 'entity-form-title'}>
              {editingKey ? `Editing wallet: ${editingKey}` : 'Add new wallet'}
            </h4>
            <form className="form-inline" onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Email</label>
                <input
                  className="input"
                  type="email"
                  value={formEmail}
                  onChange={(e) => {
                    setError('');
                    setFormEmail(e.target.value);
                  }}
                  disabled={!!editingKey}
                  required
                />
              </div>
              <div className="form-group">
                <label>Crypto currency</label>
                <select
                  className="input"
                  value={formCurrency}
                  onChange={(e) => {
                    setError('');
                    setFormCurrency(e.target.value);
                  }}
                  disabled={!!editingKey}
                  required
                >
                  {CRYPTO_CURRENCIES.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Amount</label>
                <input
                  className="input"
                  type="number"
                  step="any"
                  value={formAmount}
                  onChange={(e) => {
                    setError('');
                    setFormAmount(e.target.value);
                  }}
                  required
                />
              </div>
              <div className="form-inline-action">
                <label className="spacer-label">Action</label>
                <div>
                  <button className={editingKey ? 'btn btn-primary' : 'btn btn-success'} type="submit">
                    {editingKey ? 'Save changes' : 'Add wallet'}
                  </button>
                  <button className="btn btn-secondary" type="button" onClick={resetForm}>
                    Cancel
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={!!pendingDelete}
        title="Delete wallet"
        message={
          pendingDelete &&
          `Are you sure you want to delete the ${pendingDelete.cryptoCurrencyCode} wallet for ${pendingDelete.email}?`
        }
        onConfirm={confirmDelete}
        onCancel={() => setPendingDelete(null)}
      />
    </div>
  );
}
