import { useEffect, useState } from 'react';
import api, { getErrorMessage } from '../services/api';
import { useAuth } from '../context/AuthContext';
import ConfirmDialog from './ConfirmDialog';

const ALL_ROLES = ['OWNER', 'ADMIN', 'USER'];

export default function UsersManagement({ mode, size, onUsersChanged }) {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const [showAddForm, setShowAddForm] = useState(false);
  const [editingEmail, setEditingEmail] = useState(null);
  const [pendingDeleteEmail, setPendingDeleteEmail] = useState(null);
  const [formEmail, setFormEmail] = useState('');
  const [formPassword, setFormPassword] = useState('');
  const [formRole, setFormRole] = useState(mode === 'admin' ? 'USER' : 'OWNER');

  const isFormOpen = showAddForm || !!editingEmail;

  const ownerExistsElsewhere = users.some((u) => u.role === 'OWNER' && u.email !== editingEmail);
  const availableRoles =
    mode === 'admin' ? ['USER'] : ownerExistsElsewhere ? ['ADMIN', 'USER'] : ALL_ROLES;

  useEffect(() => {
    if (!availableRoles.includes(formRole)) {
      setFormRole(availableRoles[0]);
    }
  }, [availableRoles.join(',')]);

  async function loadUsers() {
    setLoading(true);
    setError('');
    try {
      const res = await api.get('/users');
      setUsers(res.data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadUsers();
  }, []);

  const visibleUsers = mode === 'admin' ? users.filter((u) => u.role === 'USER') : users;

  function resetForm() {
    setError('');
    setShowAddForm(false);
    setEditingEmail(null);
    setFormEmail('');
    setFormPassword('');
    setFormRole(mode === 'admin' ? 'USER' : 'OWNER');
  }

  function startAdd() {
    setError('');
    setEditingEmail(null);
    setFormEmail('');
    setFormPassword('');
    setFormRole(mode === 'admin' ? 'USER' : 'OWNER');
    setShowAddForm(true);
  }

  function startEdit(u) {
    setError('');
    setShowAddForm(false);
    setEditingEmail(u.email);
    setFormEmail(u.email);
    setFormPassword('');
    setFormRole(u.role);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      const body = { email: formEmail, password: formPassword, role: formRole };
      const isNewUser = !editingEmail;
      if (editingEmail) {
        await api.put('/users', body);
      } else {
        await api.post('/users', body);
      }
      resetForm();
      await loadUsers();
      if (isNewUser) {
        onUsersChanged?.();
      }
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }

  async function handleDelete(email) {
    setError('');
    try {
      await api.delete('/users', { params: { email } });
      await loadUsers();
      onUsersChanged?.();
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }

  function confirmDelete() {
    const email = pendingDeleteEmail;
    setPendingDeleteEmail(null);
    handleDelete(email);
  }

  return (
    <div className={size === 'lg' ? 'card card-lg' : 'card'}>
      <h3 className="section-title">Users</h3>

      {loading && <p className="muted">Loading...</p>}
      {error && <p className="alert alert-error">{error}</p>}

      <table className="table">
        <thead>
          <tr>
            <th>Email</th>
            <th>Password</th>
            <th>Role</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {visibleUsers.map((u) => (
            <tr key={u.email}>
              <td>{u.email}</td>
              <td>******</td>
              <td>{u.role}</td>
              <td>
                <button className="btn btn-secondary" onClick={() => startEdit(u)}>
                  Edit
                </button>
                {u.email.toLowerCase() !== currentUser.email.toLowerCase() && (
                  <button className="btn btn-danger" onClick={() => setPendingDeleteEmail(u.email)}>
                    Delete
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {!isFormOpen && (
        <div className="add-toggle-wrapper">
          <button className="btn btn-success" onClick={startAdd}>
            + Add user
          </button>
        </div>
      )}

      {isFormOpen && (
        <div className="entity-form-wrapper">
          <div className={editingEmail ? 'entity-form entity-form-editing' : 'entity-form'}>
            <h4 className={editingEmail ? 'entity-form-title entity-form-title-editing' : 'entity-form-title'}>
              {editingEmail ? `Editing user: ${editingEmail}` : 'Add new user'}
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
                  disabled={!!editingEmail}
                  required
                />
              </div>
              <div className="form-group">
                <label>Password</label>
                <input
                  className="input"
                  type="text"
                  value={formPassword}
                  onChange={(e) => {
                    setError('');
                    setFormPassword(e.target.value);
                  }}
                  placeholder={editingEmail ? 'New password (optional)' : ''}
                  required={!editingEmail}
                />
              </div>
              <div className="form-group">
                <label>Role</label>
                <select
                  className="input"
                  value={formRole}
                  onChange={(e) => {
                    setError('');
                    setFormRole(e.target.value);
                  }}
                  disabled={availableRoles.length === 1}
                >
                  {availableRoles.map((r) => (
                    <option key={r} value={r}>
                      {r}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-inline-action">
                <label className="spacer-label">Action</label>
                <div>
                  <button className={editingEmail ? 'btn btn-primary' : 'btn btn-success'} type="submit">
                    {editingEmail ? 'Save changes' : 'Add user'}
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
        open={!!pendingDeleteEmail}
        title="Delete user"
        message={`Are you sure you want to delete the user ${pendingDeleteEmail}?`}
        onConfirm={confirmDelete}
        onCancel={() => setPendingDeleteEmail(null)}
      />
    </div>
  );
}
