import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import NavBar from './components/NavBar';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import ExchangeRatesPage from './pages/ExchangeRatesPage';

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <NavBar />
        <div className="app-container">
          <Routes>
            <Route path="/" element={<ExchangeRatesPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <DashboardPage />
                </ProtectedRoute>
              }
            />
          </Routes>
        </div>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
