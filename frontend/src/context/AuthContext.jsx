import { createContext, useContext, useState } from 'react';
import api, { basicAuthHeader, setCredentials, clearCredentials } from '../services/api';

const AuthContext = createContext(null);
const STORAGE_KEY = 'auth';

function loadStoredAuth() {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = loadStoredAuth();
    if (!stored) return null;
    setCredentials(stored.email, stored.password);
    return { email: stored.email, role: stored.role };
  });

  async function login(email, password) {
    const response = await api.get('/login', { headers: basicAuthHeader(email, password) });
    setCredentials(email, password);
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ email, password, role: response.data.role }));
    setUser(response.data);
    return response.data;
  }

  function logout() {
    clearCredentials();
    sessionStorage.removeItem(STORAGE_KEY);
    setUser(null);
  }

  const value = {
    user,
    isAuthenticated: user !== null,
    login,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
