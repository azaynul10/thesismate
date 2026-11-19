import { FormEvent, useCallback, useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import { TRACK_LABELS } from '../api/groups';
import { Allocation, ROLE_LABELS, SupervisionRequest, supervisionApi } from '../api/supervision';
import { AuthUser } from './LoginPage';

interface Props {
  user: AuthUser | null;
  authChecked: boolean;
}

function messageOf(e: unknown): string {
  return e instanceof ApiError ? e.message : 'Something went wrong. Please try again.';
}

function memberName(m: unknown): string {
  if (typeof m === 'string') return m;
  if (m && typeof m === 'object' && 'fullName' in m) {
    return String((m as { fullName: unknown }).fullName);
  }
  return JSON.stringify(m);
}

function memberList(members: unknown[]): string {
  return members.map(memberName).join(', ');
}

export default function SupervisorRequestsPage({ user, authChecked }: Props) {
  const [requests, setRequests] = useState<SupervisionRequest[]>([]);
  const [groups, setGroups] = useState<Allocation[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [rejectingId, setRejectingId] = useState<number | null>(null);
  const [reason, setReason] = useState('');

  const isSupervisor = user?.role === 'SUPERVISOR';

  const load = useCallback(async () => {
    try {
      const [reqs, grps] = await Promise.all([
        supervisionApi.pendingForSupervisor(),
        supervisionApi.myGroups(),
      ]);
      setRequests(reqs);
      setGroups(grps);
    } catch (e) {
      setError(messageOf(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isSupervisor) {
      void load();
    }
  }, [isSupervisor, load]);

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
      await load();
      return false;
    } finally {
      setBusy(false);
    }
  }

  function handleAccept(request: SupervisionRequest) {
    void run(() => supervisionApi.accept(request.id), 'Request accepted.');
  }

  function startReject(request: SupervisionRequest) {
    setRejectingId(request.id);
    setReason('');
  }

  async function handleReject(e: FormEvent, request: SupervisionRequest) {
    e.preventDefault();
    const ok = await run(
      () => supervisionApi.reject(request.id, reason.trim()),
      'Request rejected.',
    );
    if (ok) {
      setRejectingId(null);
      setReason('');
    }
  }

  if (!authChecked) {
    return <p className="text-secondary">Loading...</p>;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (!isSupervisor) {
    return (
      <div className="tm-card p-4">
        <h1 className="h4 mb-2">Supervision requests</h1>
        <p className="text-secondary mb-0">
          This page is for supervisors. Your role is {user.role}.
        </p>
      </div>
    );
  }

  return (
    <div>
      <h1 className="h3 mb-3">Supervision requests</h1>

      <div aria-live="polite">
        {error && (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        )}
        {notice && <div className="alert alert-success">{notice}</div>}
      </div>

      {loading ? (
        <p className="text-secondary">Loading...</p>
      ) : (
        <>
          <div className="tm-card p-4 mb-4">
            <h2 className="h5 mb-3">Pending requests ({requests.length})</h2>
            {requests.length === 0 ? (
              <p className="text-secondary mb-0">No pending requests.</p>
            ) : (
              <ul className="list-group list-group-flush">
                {requests.map((r) => (
                  <li key={r.id} className="list-group-item px-0">
                    <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
                      <div>
                        <strong>{TRACK_LABELS[r.track]} group</strong>{' '}
                        <span className="text-secondary small">{r.semester}</span>
                      </div>
                      <span className="badge text-bg-light border text-dark">
                        as {ROLE_LABELS[r.role]}
                      </span>
                    </div>
                    <div className="small text-secondary mt-1">
                      Members: {memberList(r.groupMembers)}
                    </div>
                    <p className="small mb-1 mt-2" style={{ whiteSpace: 'pre-wrap' }}>
                      {r.pitch}
                    </p>
                    <div className="small text-secondary">
                      Received {new Date(r.createdAt).toLocaleDateString()} &middot; expires{' '}
                      {new Date(r.expiresAt).toLocaleDateString()}
                    </div>

                    {rejectingId === r.id ? (
                      <form onSubmit={(e) => void handleReject(e, r)} className="mt-3">
                        <label htmlFor={`reject-reason-${r.id}`} className="form-label small">
                          Reason for rejecting (the group will see this)
                        </label>
                        <textarea
                          id={`reject-reason-${r.id}`}
                          className="form-control mb-2"
                          rows={2}
                          value={reason}
                          onChange={(e) => setReason(e.target.value)}
                          required
                        />
                        <div className="d-flex gap-2">
                          <button
                            type="submit"
                            className="btn btn-danger btn-sm"
                            disabled={busy || !reason.trim()}
                          >
                            Reject request
                          </button>
                          <button
                            type="button"
                            className="btn btn-outline-secondary btn-sm"
                            disabled={busy}
                            onClick={() => setRejectingId(null)}
                          >
                            Cancel
                          </button>
                        </div>
                      </form>
                    ) : (
                      <div className="d-flex gap-2 mt-3">
                        <button
                          type="button"
                          className="btn btn-primary btn-sm"
                          disabled={busy}
                          onClick={() => handleAccept(r)}
                        >
                          Accept
                        </button>
                        <button
                          type="button"
                          className="btn btn-outline-secondary btn-sm"
                          disabled={busy}
                          onClick={() => startReject(r)}
                        >
                          Reject
                        </button>
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="tm-card p-4">
            <h2 className="h5 mb-3">Groups you supervise ({groups.length})</h2>
            {groups.length === 0 ? (
              <p className="text-secondary mb-0">You have not accepted any groups yet.</p>
            ) : (
              <ul className="list-group list-group-flush">
                {groups.map((g) => (
                  <li key={g.id} className="list-group-item px-0">
                    <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
                      <div>
                        <strong>{TRACK_LABELS[g.track]} group</strong>{' '}
                        <span className="text-secondary small">{g.semester}</span>
                      </div>
                      <span className="badge text-bg-success">{ROLE_LABELS[g.role]}</span>
                    </div>
                    <div className="small text-secondary mt-1">
                      Members: {memberList(g.groupMembers)}
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </>
      )}
    </div>
  );
}