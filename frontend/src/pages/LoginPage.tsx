import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, ApiError } from '../api/client';

export interface AuthUser {
  id: number;
  email: string;
  fullName: string;
  role: 'STUDENT' | 'SUPERVISOR' | 'COORDINATOR' | 'CHAIRPERSON' | 'EVALUATOR';
  studentId: string | null;
}

interface Props {
  onLogin: (user: AuthUser) => void;
}

export default function LoginPage({ onLogin }: Props) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const user = await api<AuthUser>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      });
      onLogin(user);
      navigate(user.role === 'SUPERVISOR' ? '/supervisor/capacity' : '/');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Login failed');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="row justify-content-center">
      <div className="col-md-5">
        <div className="card shadow-sm">
          <div className="card-body">
            <h2 className="h4 mb-3">Sign in</h2>
            {error && (
              <div className="alert alert-danger" role="alert">
                {error}
              </div>
            )}
            <form onSubmit={handleSubmit}>
              <div className="mb-3">
                <label htmlFor="login-email" className="form-label">Email</label>
                <input
                  id="login-email"
                  type="email"
                  className="form-control"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  autoComplete="username"
                />
              </div>
              <div className="mb-3">
                <label htmlFor="login-password" className="form-label">Password</label>
                <input
                  id="login-password"
                  type="password"
                  className="form-control"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  autoComplete="current-password"
                />
              </div>
              <button type="submit" className="btn btn-primary w-100" disabled={submitting}>
                {submitting ? 'Signing in...' : 'Sign in'}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
