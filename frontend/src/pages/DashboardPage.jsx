import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import UsersManagement from '../components/UsersManagement';
import BankAccounts from '../components/BankAccounts';
import CryptoWallets from '../components/CryptoWallets';
import CurrencyConversion from '../components/CurrencyConversion';
import TradeService from '../components/TradeService';

export default function DashboardPage() {
  const { user } = useAuth();
  const [accountsRefreshSignal, setAccountsRefreshSignal] = useState(0);
  const [walletsRefreshSignal, setWalletsRefreshSignal] = useState(0);

  return (
    <div>
      <div className="user-greeting">
        <span className="greeting-line">Welcome,</span>
        <span className="greeting-role">{user.role}</span>
        <p className="greeting-sub">You are logged in as {user.email}.</p>
      </div>

      {user.role === 'OWNER' && <UsersManagement mode="owner" size="lg" />}

      {user.role === 'ADMIN' && (
        <>
          <UsersManagement
            mode="admin"
            size="lg"
            onUsersChanged={() => {
              setAccountsRefreshSignal((s) => s + 1);
              setWalletsRefreshSignal((s) => s + 1);
            }}
          />
          <BankAccounts mode="admin" size="lg" refreshSignal={accountsRefreshSignal} />
          <CryptoWallets mode="admin" size="lg" refreshSignal={walletsRefreshSignal} />
        </>
      )}

      {user.role === 'USER' && (
        <>
          <div className="two-col-grid">
            <BankAccounts mode="user" size="lg" refreshSignal={accountsRefreshSignal} />
            <CryptoWallets mode="user" size="lg" refreshSignal={walletsRefreshSignal} />
          </div>

          <CurrencyConversion size="lg" onSuccess={() => setAccountsRefreshSignal((s) => s + 1)} />
          <TradeService
            size="lg"
            onSuccess={() => {
              setAccountsRefreshSignal((s) => s + 1);
              setWalletsRefreshSignal((s) => s + 1);
            }}
          />
        </>
      )}
    </div>
  );
}
