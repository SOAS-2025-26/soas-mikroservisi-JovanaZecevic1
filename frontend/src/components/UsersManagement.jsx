import { useEffect, useState } from 'react';
import api, { getErrorMessage } from '../services/api';

const ALL_ROLES = ['OWNER', 'ADMIN', 'USER'];

export default function UsersManagement({ mode }) {
  const [users, setUsers] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const [editingEmail, setEditingEmail] = useState(null);
  const [formEmail, setFormEmail] = useState('');
  const [formPassword, setFormPassword] = useState('');
  const [formRole, setFormRole] = useState(mode === 'admin' ? 'USER' : 'OWNER');

  const ownerExistsElsewhere = users.some((u) => u.role === 'OWNER' && u.email !== editingEmail);
  const availableRoles =
    mode === 'admin' ? ['USER'] : ownerExistsElsewhere ? ['ADMIN', 'USER'] : ALL_ROLES;

  useEffect(() => {
    if (!availableRoles.includes(formRole)) {
      setFormRole(availableRoles[0]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const visibleUsers = mode === 'admin' ? users.filter((u) => u.role === 'USER') : users;

  function resetForm() {
    setEditingEmail(null);
    setFormEmail('');
    setFormPassword('');
    setFormRole(mode === 'admin' ? 'USER' : 'OWNER');
  }

  function startEdit(u) {
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
      if (editingEmail) {
        await api.put('/users', body);
      } else {
        await api.post('/users', body);
      }
      resetForm();
      await loadUsers();
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }

  async function handleDelete(email) {
    setError('');
    try {
      await api.delete('/users', { params: { email } });
      await loadUsers();
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }

  return (
    <div>
      <h3>{mode === 'admin' ? 'Users (USER role)' : 'Users'}</h3>

      {loading && <p>Loading...</p>}
      {error && <p style={{ color: 'red' }}>{error}</p>}

      <table border="1" cellPadding="4">
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
                <button onClick={() => startEdit(u)}>Edit</button>
                <button onClick={() => handleDelete(u.email)}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <h4>{editingEmail ? `Editing user: ${editingEmail}` : 'Add user'}</h4>
      <form onSubmit={handleSubmit}>
        <div>
          <label>Email:</label>
          <input
            type="email"
            value={formEmail}
            onChange={(e) => setFormEmail(e.target.value)}
            disabled={!!editingEmail}
            required
          />
        </div>
        <div>
          <label>Password:</label>
          <input
            type="text"
            value={formPassword}
            onChange={(e) => setFormPassword(e.target.value)}
            placeholder={editingEmail ? 'Leave blank to keep current password' : ''}
            required={!editingEmail}
          />
        </div>
        <div>
          <label>Role:</label>
          <select
            value={formRole}
            onChange={(e) => setFormRole(e.target.value)}
            disabled={availableRoles.length === 1}
          >
            {availableRoles.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>
        </div>
        <button type="submit">{editingEmail ? 'Save changes' : 'Add user'}</button>
        {editingEmail && (
          <button type="button" onClick={resetForm}>
            Cancel
          </button>
        )}
      </form>
    </div>
  );
}
