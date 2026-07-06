import { useAuth } from '../context/AuthContext';
import UsersManagement from '../components/UsersManagement';
import BankAccounts from '../components/BankAccounts';
import CryptoWallets from '../components/CryptoWallets';
import CurrencyConversion from '../components/CurrencyConversion';
import TradeService from '../components/TradeService';

export default function DashboardPage() {
  const { user } = useAuth();

  return (
    <div>
      <h2>Welcome, {user.role}</h2>
      <p>You are logged in as {user.email}.</p>

      {user.role === 'OWNER' && <UsersManagement mode="owner" />}

      {user.role === 'ADMIN' && (
        <>
          <UsersManagement mode="admin" />
          <BankAccounts mode="admin" />
          <CryptoWallets mode="admin" />
        </>
      )}

      {user.role === 'USER' && (
        <>
          <BankAccounts mode="user" />
          <CryptoWallets mode="user" />
          <CurrencyConversion />
          <TradeService />
        </>
      )}
    </div>
  );
}
