import { useAuth } from '../context/AuthContext';

export default function DashboardPage() {
  const { user } = useAuth();

  return (
    <div>
      <h2>Dobrodošli, {user.role}</h2>
      <p>Ulogovani ste kao {user.email}.</p>
    </div>
  );
}
