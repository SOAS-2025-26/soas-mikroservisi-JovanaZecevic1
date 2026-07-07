import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function NavBar() {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/');
  }

  return (
    <nav className="navbar">
      <div className="navbar-links">
        <span className="navbar-brand">
          <span>Exchange</span>
        </span>
        <Link to="/">Exchange Rates</Link>
        {isAuthenticated && <Link to="/dashboard">Dashboard</Link>}
      </div>
      <div className="navbar-links">
        {isAuthenticated ? (
          <>
            <span className="navbar-user">
              {user.email}
              <span className={`role-badge role-badge-${user.role.toLowerCase()}`}>{user.role}</span>
            </span>
            <button className="btn btn-secondary" onClick={handleLogout}>
              Log out
            </button>
          </>
        ) : (
          <Link className="btn btn-primary" to="/login">
            Log in
          </Link>
        )}
      </div>
    </nav>
  );
}
