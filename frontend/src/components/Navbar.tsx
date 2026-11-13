import { Link, NavLink } from 'react-router-dom';
import { AuthUser } from '../pages/LoginPage';

interface Props {
  user: AuthUser | null;
  onLogout: () => void;
}

export default function Navbar({ user, onLogout }: Props) {
  return (
    <nav className="navbar navbar-expand bg-white border-bottom" aria-label="Main navigation">
      <div className="container">
        <Link to="/" className="navbar-brand fw-semibold text-dark">
          ThesisMate
        </Link>
        <div className="navbar-nav me-auto">
          {user?.role === 'SUPERVISOR' && (
            <NavLink
              to="/supervisor/capacity"
              className={({ isActive }) =>
                `nav-link px-3 ${isActive ? 'fw-semibold text-primary' : 'text-secondary'}`
              }
            >
              My Capacity
            </NavLink>
          )}
        </div>
        <div className="d-flex align-items-center gap-2">
          {user ? (
            <>
              <span className="small text-secondary d-none d-sm-inline">
                {user.fullName}
              </span>
              <span className="badge text-bg-light border text-dark">{user.role}</span>
              <button
                type="button"
                className="btn btn-outline-secondary btn-sm"
                onClick={onLogout}
              >
                Sign out
              </button>
            </>
          ) : (
            <Link to="/login" className="btn btn-primary btn-sm">Sign in</Link>
          )}
        </div>
      </div>
    </nav>
  );
}
