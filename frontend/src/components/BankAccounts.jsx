import { useEffect, useState } from 'react';
import api, { getErrorMessage } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { formatNumber, roundForInput } from '../utils/format';
import { FIAT_CURRENCIES } from '../constants/currencies';
import ConfirmDialog from './ConfirmDialog';

export default function BankAccounts({ mode, refreshSignal, size }) {
  const { user } = useAuth();
  const [accounts, setAccounts] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const [showAddForm, setShowAddForm] = useState(false);
  const [editingKey, setEditingKey] = useState(null);
  const [pendingDelete, setPendingDelete] = useState(null);
  const [formEmail, setFormEmail] = useState('');
  const [formCurrency, setFormCurrency] = useState(FIAT_CURRENCIES[0]);
  const [formAmount, setFormAmount] = useState('');

  const isFormOpen = showAddForm || !!editingKey;

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
  }, [refreshSignal]);

  function resetForm() {
    setError('');
    setShowAddForm(false);
    setEditingKey(null);
    setFormEmail('');
    setFormCurrency(FIAT_CURRENCIES[0]);
    setFormAmount('');
  }

  function startAdd() {
    setError('');
    setEditingKey(null);
    setFormEmail('');
    setFormCurrency(FIAT_CURRENCIES[0]);
    setFormAmount('');
    setShowAddForm(true);
  }

  function startEdit(a) {
    setError('');
    setShowAddForm(false);
    setEditingKey(`${a.email}|${a.currencyCode}`);
    setFormEmail(a.email);
    setFormCurrency(a.currencyCode);
    setFormAmount(roundForInput(a.amount));
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

  function confirmDelete() {
    const { email, currencyCode } = pendingDelete;
    setPendingDelete(null);
    handleDelete(email, currencyCode);
  }

  const groupedAccounts =
    mode === 'admin'
      ? Object.entries(
          accounts.reduce((acc, a) => {
            (acc[a.email] ||= []).push(a);
            return acc;
          }, {})
        )
      : null;

  return (
    <div className={size === 'lg' ? 'card card-lg' : 'card'}>
      <h3 className="section-title">{mode === 'admin' ? 'Bank accounts' : 'My Bank Account'}</h3>

      {loading && <p className="muted">Loading...</p>}
      {error && <p className="alert alert-error">{error}</p>}

      {mode === 'admin' ? (
        groupedAccounts.length === 0 ? (
          <p className="muted">No accounts yet.</p>
        ) : (
          groupedAccounts.map(([email, rows]) => (
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
                  {rows.map((a) => (
                    <tr key={`${a.email}|${a.currencyCode}`}>
                      <td>{a.currencyCode}</td>
                      <td>{formatNumber(a.amount)}</td>
                      <td>
                        <button className="btn btn-secondary" onClick={() => startEdit(a)}>
                          Edit
                        </button>
                        <button
                          className="btn btn-danger"
                          onClick={() => setPendingDelete({ email: a.email, currencyCode: a.currencyCode })}
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
            {accounts.map((a) => (
              <tr key={`${a.email}|${a.currencyCode}`}>
                <td>{a.currencyCode}</td>
                <td>{formatNumber(a.amount)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {mode === 'admin' && !isFormOpen && (
        <div className="add-toggle-wrapper">
          <button className="btn btn-success" onClick={startAdd}>
            + Add account
          </button>
        </div>
      )}

      {mode === 'admin' && isFormOpen && (
        <div className="entity-form-wrapper">
          <div className={editingKey ? 'entity-form entity-form-editing' : 'entity-form'}>
            <h4 className={editingKey ? 'entity-form-title entity-form-title-editing' : 'entity-form-title'}>
              {editingKey ? `Editing account: ${editingKey}` : 'Add new account'}
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
                <label>Currency</label>
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
                  {FIAT_CURRENCIES.map((c) => (
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
                    {editingKey ? 'Save changes' : 'Add account'}
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
        title="Delete account"
        message={
          pendingDelete &&
          `Are you sure you want to delete the ${pendingDelete.currencyCode} account for ${pendingDelete.email}?`
        }
        onConfirm={confirmDelete}
        onCancel={() => setPendingDelete(null)}
      />
    </div>
  );
}
