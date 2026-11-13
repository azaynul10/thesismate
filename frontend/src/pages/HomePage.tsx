import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { AuthUser } from './LoginPage';

interface Props {
  user: AuthUser | null;
}

export default function HomePage({ user }: Props) {
  const [status, setStatus] = useState('checking...');

  useEffect(() => {
    api<{ status: string }>('/health')
      .then((r) => setStatus(r.status))
      .catch(() => setStatus('unreachable'));
  }, []);

  const isUp = status === 'UP';

  return (
    <div className="tm-card p-4">
      <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
        <div>
          <h1 className="h3 mb-1">ThesisMate</h1>
          <p className="text-secondary mb-3">
            CSE400 Thesis, Project &amp; Internship Management Platform
          </p>
        </div>
        <span
          className={`badge ${isUp ? 'text-bg-success' : 'text-bg-secondary'}`}
          role="status"
        >
          Backend: {status}
        </span>
      </div>

      {user ? (
        <div className="border-top pt-3 mt-2 d-flex flex-wrap align-items-center justify-content-between gap-2">
          <div className="small text-secondary">
            Signed in as <strong className="text-dark">{user.fullName}</strong> ({user.role})
          </div>
          {user.role === 'SUPERVISOR' && (
            <Link to="/supervisor/capacity" className="btn btn-primary btn-sm">
              Open My Capacity
            </Link>
          )}
        </div>
      ) : (
        <div className="border-top pt-3 mt-2">
          <Link to="/login" className="btn btn-primary btn-sm">
            Sign in to continue
          </Link>
        </div>
      )}
    </div>
  );
}
