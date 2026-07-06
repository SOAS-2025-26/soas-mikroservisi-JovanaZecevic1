import { createContext, useContext, useState } from 'react';
import api, { basicAuthHeader, setCredentials, clearCredentials } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null); // { email, role }

  async function login(email, password) {
    const response = await api.get('/login', { headers: basicAuthHeader(email, password) });
    setCredentials(email, password);
    setUser(response.data);
    return response.data;
  }

  function logout() {
    clearCredentials();
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
