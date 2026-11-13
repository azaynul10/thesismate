import { useEffect, useState } from 'react';
import { Routes, Route } from 'react-router-dom';
import { api } from './api/client';
import Navbar from './components/Navbar';
import HomePage from './pages/HomePage';
import LoginPage, { AuthUser } from './pages/LoginPage';
import MyCapacityPage from './pages/MyCapacityPage';

export default function App() {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [authChecked, setAuthChecked] = useState(false);

  useEffect(() => {
    api<AuthUser>('/auth/me')
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setAuthChecked(true));
  }, []);

  async function handleLogout() {
    try {
      await api<void>('/auth/logout', { method: 'POST' });
    } finally {
      setUser(null);
    }
  }

  return (
    <>
      <Navbar user={user} onLogout={handleLogout} />
      <main className="container py-4" style={{ maxWidth: '960px' }}>
        <Routes>
          <Route path="/" element={<HomePage user={user} />} />
          <Route path="/login" element={<LoginPage onLogin={setUser} />} />
          <Route
            path="/supervisor/capacity"
            element={<MyCapacityPage user={user} authChecked={authChecked} />}
          />
        </Routes>
      </main>
    </>
  );
}
