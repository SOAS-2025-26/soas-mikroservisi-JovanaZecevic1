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
    <nav>
      <Link to="/">Kursevi</Link>
      {' | '}
      {isAuthenticated ? (
        <>
          <Link to="/dashboard">Dashboard</Link>
          {' | '}
          <span>{user.email} ({user.role})</span>
          {' | '}
          <button onClick={handleLogout}>Odjava</button>
        </>
      ) : (
        <Link to="/login">Prijava</Link>
      )}
    </nav>
  );
}
