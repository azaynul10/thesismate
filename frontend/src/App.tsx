import { useEffect, useState } from 'react';
import { Routes, Route } from 'react-router-dom';
import { api } from './api/client';
import Navbar from './components/Navbar';
import GroupsPage from './pages/GroupsPage';
import HomePage from './pages/HomePage';
import LoginPage, { AuthUser } from './pages/LoginPage';
import MilestonesPage from './pages/MilestonesPage';
import MyCapacityPage from './pages/MyCapacityPage';
import ReportsPage from './pages/ReportsPage';
import StudentRecordsPage from './pages/StudentRecordsPage';
import SubmissionWindowsPage from './pages/SubmissionWindowsPage';
import SupervisionPage from './pages/SupervisionPage';
import SupervisorDirectoryPage from './pages/SupervisorDirectoryPage';
import SupervisorProgressPage from './pages/SupervisorProgressPage';
import SupervisorRequestsPage from './pages/SupervisorRequestsPage';
import WeeklyReportsPage from './pages/WeeklyReportsPage';

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
            path="/supervisors"
            element={<SupervisorDirectoryPage user={user} authChecked={authChecked} />}
          />
          <Route
            path="/groups"
            element={<GroupsPage user={user} authChecked={authChecked} />}
          />
          <Route
            path="/reports"
            element={<ReportsPage user={user} authChecked={authChecked} />}
          />
          <Route
            path="/supervisor/capacity"
            element={<MyCapacityPage user={user} authChecked={authChecked} />}
          />
          <Route
            path="/coordinator/student-records"
            element={<StudentRecordsPage user={user} authChecked={authChecked} />}
          />
          <Route
            path="/coordinator/submission-windows"
            element={<SubmissionWindowsPage user={user} authChecked={authChecked} />}
          />
          <Route
            path="/groups/supervision"
            element={<SupervisionPage user={user} authChecked={authChecked} />}
          />
          <Route
            path="/supervisor/requests"
            element={<SupervisorRequestsPage user={user} authChecked={authChecked} />}
          />
          <Route
            path="/milestones"
            element={<MilestonesPage user={user} authChecked={authChecked} />}
          />
          <Route
            path="/weekly-reports"
            element={<WeeklyReportsPage user={user} authChecked={authChecked} />}
          />
          <Route
            path="/supervisor/progress"
            element={<SupervisorProgressPage user={user} authChecked={authChecked} />}
          />
        </Routes>
      </main>
    </>
  );
}