import { FormEvent, useCallback, useEffect, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import { Group, TRACK_LABELS, groupsApi } from '../api/groups';
import {
  Allocation,
  ROLE_LABELS,
  RequestStatus,
  SupervisionRequest,
  SupervisorOption,
  SupervisorRole,
  supervisionApi,
} from '../api/supervision';
import { AuthUser } from './LoginPage';

interface Props {
  user: AuthUser | null;
  authChecked: boolean;
}

const MAX_PENDING = 3;

const STATUS_CLASS: Record<RequestStatus, string> = {
  PENDING: 'text-bg-warning',
  ACCEPTED: 'text-bg-success',
  REJECTED: 'text-bg-danger',
  WITHDRAWN: 'text-bg-secondary',
  EXPIRED: 'text-bg-secondary',
};

function messageOf(e: unknown): string {
  return e instanceof ApiError ? e.message : 'Something went wrong. Please try again.';
}

export default function SupervisionPage({ user, authChecked }: Props) {
  const [group, setGroup] = useState<Group | null>(null);
  const [requests, setRequests] = useState<SupervisionRequest[]>([]);
  const [allocations, setAllocations] = useState<Allocation[]>([]);
  const [supervisors, setSupervisors] = useState<SupervisorOption[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [supervisorId, setSupervisorId] = useState('');
  const [role, setRole] = useState<SupervisorRole>('SUPERVISOR');
  const [pitch, setPitch] = useState('');

  const userId = user?.id;
  const isStudent = user?.role === 'STUDENT';

  const load = useCallback(async () => {
    try {
      const g = await groupsApi.myGroup().catch((e: unknown) => {
        if (e instanceof ApiError && e.status === 404) return null;
        throw e;
      });
      setGroup(g);
      if (g) {
        const [reqs, allocs] = await Promise.all([
          supervisionApi.requests(g.id),
          supervisionApi.allocations(g.id),
        ]);
        setRequests(reqs);
        setAllocations(allocs);
        if (g.leaderId === userId) {
          setSupervisors(await supervisionApi.availableSupervisors(g.semester, g.track));
        }
      }
    } catch (e) {
      setError(messageOf(e));
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    if (isStudent) {
      void load();
    }
  }, [isStudent, load]);

  async function run(action: () => Promise<unknown>, success: string): Promise<boolean> {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await action();
      setNotice(success);
      await load();
      return true;
    } catch (e) {
      setError(messageOf(e));
      return false;
    } finally {
      setBusy(false);
    }
  }

  async function handleSend(e: FormEvent) {
    e.preventDefault();
    if (!group) return;
    const ok = await run(
      () => supervisionApi.send(group.id, Number(supervisorId), role, pitch.trim()),
      'Request sent.',
    );
    if (ok) {
      setPitch('');
      setSupervisorId('');
    }
  }

  function handleWithdraw(request: SupervisionRequest) {
    void run(() => supervisionApi.withdraw(request.id), 'Request withdrawn.');
  }

  if (!authChecked) {
    return <p className="text-secondary">Loading...</p>;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (!isStudent) {
    return (
      <div className="tm-card p-4">
        <h1 className="h4 mb-2">Supervision</h1>
        <p className="text-secondary mb-0">This page is for students. Your role is {user.role}.</p>
      </div>
    );
  }
  if (loading) {
    return <p className="text-secondary">Loading...</p>;
  }
  if (group === null) {
    return (
      <div className="tm-card p-4">
        <h1 className="h4 mb-2">Supervision</h1>
        <p className="text-secondary">
          Your group needs to exist before you can ask a supervisor.
        </p>
        <Link to="/groups" className="btn btn-primary btn-sm">
          Go to My Group
        </Link>
      </div>
    );
  }

  const isLeader = group.leaderId === user.id;
  const pendingCount = requests.filter((r) => r.status === 'PENDING').length;

  return (
    <div>
      <h1 className="h3 mb-1">Supervision</h1>
      <p className="text-secondary mb-3">
        {TRACK_LABELS[group.track]} group, {group.semester}
      </p>

      <div aria-live="polite">
        {error && (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        )}
        {notice && <div className="alert alert-success">{notice}</div>}
      </div>

      <div className="tm-card p-4 mb-4">
        <h2 className="h5 mb-3">Your supervisors</h2>
        {allocations.length === 0 ? (
          <p className="text-secondary mb-0">No supervisor has accepted your group yet.</p>
        ) : (
          <ul className="list-group list-group-flush">
            {allocations.map((a) => (
              <li
                key={a.id}
                className="list-group-item px-0 d-flex justify-content-between align-items-center"
              >
                <span>{a.supervisorName}</span>
                <span className="badge text-bg-success">{ROLE_LABELS[a.role]}</span>
              </li>
            ))}
          </ul>
        )}
      </div>

      {isLeader ? (
        <div className="tm-card p-4 mb-4">
          <h2 className="h5 mb-1">Send a request</h2>
          <p className="small text-secondary">
            {pendingCount} of {MAX_PENDING} pending requests used. A supervisor can have only one
            pending request from your group at a time, and requests expire after 7 days.
          </p>
          {supervisors.length === 0 ? (
            <p className="text-secondary mb-0">
              No supervisors with free slots for {TRACK_LABELS[group.track]} in {group.semester}.
            </p>
          ) : (
            <form onSubmit={handleSend} className="row g-3">
              <div className="col-md-8">
                <label htmlFor="req-supervisor" className="form-label">Supervisor</label>
                <select
                  id="req-supervisor"
                  className="form-select"
                  value={supervisorId}
                  onChange={(e) => setSupervisorId(e.target.value)}
                  required
                >
                  <option value="">Choose a supervisor</option>
                  {supervisors.map((s) => (
                    <option key={s.supervisorId} value={s.supervisorId}>
                      {s.fullName} ({s.remainingSlots} {s.remainingSlots === 1 ? 'slot' : 'slots'} open)
                    </option>
                  ))}
                </select>
              </div>
              <div className="col-md-4">
                <label htmlFor="req-role" className="form-label">Role</label>
                <select
                  id="req-role"
                  className="form-select"
                  value={role}
                  onChange={(e) => setRole(e.target.value as SupervisorRole)}
                >
                  {(Object.keys(ROLE_LABELS) as SupervisorRole[]).map((r) => (
                    <option key={r} value={r}>
                      {ROLE_LABELS[r]}
                    </option>
                  ))}
                </select>
              </div>
              <div className="col-12">
                <label htmlFor="req-pitch" className="form-label">Pitch</label>
                <textarea
                  id="req-pitch"
                  className="form-control"
                  rows={4}
                  value={pitch}
                  onChange={(e) => setPitch(e.target.value)}
                  placeholder="Describe your topic and why this supervisor is a good fit."
                  required
                />
              </div>
              <div className="col-12">
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={busy || pendingCount >= MAX_PENDING}
                >
                  Send request
                </button>
              </div>
            </form>
          )}
        </div>
      ) : (
        <p className="small text-secondary">
          Only the group leader ({group.leaderName}) can send or withdraw requests.
        </p>
      )}

      <div className="tm-card p-4">
        <h2 className="h5 mb-3">Requests</h2>
        {requests.length === 0 ? (
          <p className="text-secondary mb-0">No requests yet.</p>
        ) : (
          <ul className="list-group list-group-flush">
            {requests.map((r) => (
              <li key={r.id} className="list-group-item px-0">
                <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
                  <div>
                    <strong>{r.supervisorName}</strong>{' '}
                    <span className="text-secondary small">as {ROLE_LABELS[r.role]}</span>
                  </div>
                  <span className={`badge ${STATUS_CLASS[r.status]}`}>{r.status}</span>
                </div>
                <p className="small mb-1 mt-2" style={{ whiteSpace: 'pre-wrap' }}>
                  {r.pitch}
                </p>
                <div className="small text-secondary">
                  Sent {new Date(r.createdAt).toLocaleDateString()}
                  {r.status === 'PENDING' && (
                    <> &middot; expires {new Date(r.expiresAt).toLocaleDateString()}</>
                  )}
                </div>
                {r.status === 'REJECTED' && r.rejectionReason && (
                  <div className="small mt-1">
                    <span className="text-secondary">Reason: </span>
                    {r.rejectionReason}
                  </div>
                )}
                {isLeader && r.status === 'PENDING' && (
                  <button
                    type="button"
                    className="btn btn-outline-secondary btn-sm mt-2"
                    disabled={busy}
                    onClick={() => handleWithdraw(r)}
                  >
                    Withdraw
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}