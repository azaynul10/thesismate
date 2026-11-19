import { FormEvent, useCallback, useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import {
  DELIVERABLE_TYPES,
  DeliverableRequirement,
  DeliverableType,
  Extension,
  LatePolicy,
  PHASES,
  Phase,
  SubmissionWindow,
  TRACKS,
  Track,
  submissionsApi,
} from '../api/submissions';
import { AuthUser } from './LoginPage';

interface Props {
  user: AuthUser | null;
  authChecked: boolean;
}

function messageOf(e: unknown): string {
  if (e instanceof ApiError) {
    const details = Object.values(e.fieldErrors);
    return details.length > 0 ? `${e.message}: ${details.join(', ')}` : e.message;
  }
  return 'Something went wrong. Please try again.';
}

function label(value: string): string {
  return value.replace(/_/g, ' ');
}

function when(value: string | null): string {
  return value ? new Date(value).toLocaleString() : '-';
}

export default function SubmissionWindowsPage({ user, authChecked }: Props) {
  const isCoordinator = user?.role === 'COORDINATOR';

  const [windows, setWindows] = useState<SubmissionWindow[]>([]);
  const [requirements, setRequirements] = useState<DeliverableRequirement[]>([]);
  const [extensions, setExtensions] = useState<Extension[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  // window form
  const [semester, setSemester] = useState('Fall 2026');
  const [wPhase, setWPhase] = useState<Phase>('PRE_THESIS_1');
  const [wTrack, setWTrack] = useState<Track>('THESIS');
  const [opensAt, setOpensAt] = useState('');
  const [closesAt, setClosesAt] = useState('');
  const [latePolicy, setLatePolicy] = useState<LatePolicy>('NO_LATE');
  const [lateUntil, setLateUntil] = useState('');

  // checklist form
  const [rPhase, setRPhase] = useState<Phase>('PRE_THESIS_1');
  const [rTrack, setRTrack] = useState<Track>('THESIS');
  const [rType, setRType] = useState<DeliverableType>('REPORT');
  const [rMandatory, setRMandatory] = useState(true);

  // extension form
  const [extWindowId, setExtWindowId] = useState('');
  const [extGroupId, setExtGroupId] = useState('');
  const [extClosesAt, setExtClosesAt] = useState('');
  const [extReason, setExtReason] = useState('');

  const loadExtensions = useCallback(async (windowId: string) => {
    if (!windowId) {
      setExtensions([]);
      return;
    }
    setExtensions(await submissionsApi.listExtensions(Number(windowId)));
  }, []);

  const load = useCallback(async () => {
    try {
      const [w, r] = await Promise.all([
        submissionsApi.listWindows(),
        submissionsApi.listRequirements(),
      ]);
      setWindows(w);
      setRequirements(r);
    } catch (e) {
      setError(messageOf(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isCoordinator) {
      void load();
    }
  }, [isCoordinator, load]);

  useEffect(() => {
    if (isCoordinator) {
      loadExtensions(extWindowId).catch((e) => setError(messageOf(e)));
    }
  }, [isCoordinator, extWindowId, loadExtensions]);

  async function run(action: () => Promise<void>, done: string) {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await action();
      setNotice(done);
    } catch (e) {
      setError(messageOf(e));
    } finally {
      setBusy(false);
    }
  }

  function createWindow(e: FormEvent) {
    e.preventDefault();
    void run(async () => {
      await submissionsApi.createWindow({
        semester: semester.trim(),
        phase: wPhase,
        track: wTrack,
        opensAt,
        closesAt,
        latePolicy,
        lateUntil: latePolicy === 'LATE_ALLOWED' && lateUntil ? lateUntil : null,
      });
      await load();
    }, 'Submission window created.');
  }

  function addRequirement(e: FormEvent) {
    e.preventDefault();
    void run(async () => {
      await submissionsApi.addRequirement({
        phase: rPhase,
        track: rTrack,
        deliverableType: rType,
        mandatory: rMandatory,
      });
      await load();
    }, 'Deliverable added to the checklist.');
  }

  function removeRequirement(id: number) {
    void run(async () => {
      await submissionsApi.deleteRequirement(id);
      await load();
    }, 'Deliverable removed from the checklist.');
  }

  function grantExtension(e: FormEvent) {
    e.preventDefault();
    void run(async () => {
      await submissionsApi.grantExtension({
        groupId: Number(extGroupId),
        windowId: Number(extWindowId),
        newClosesAt: extClosesAt,
        reason: extReason,
      });
      await loadExtensions(extWindowId);
      setExtReason('');
    }, 'Extension saved.');
  }

  if (!authChecked) {
    return <p className="text-secondary">Loading...</p>;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (!isCoordinator) {
    return (
      <div className="tm-card p-4">
        <h1 className="h4 mb-2">Submission windows</h1>
        <p className="text-secondary mb-0">
          This page is for coordinators. Your role is {user.role}.
        </p>
      </div>
    );
  }

  return (
    <div>
      <h1 className="h3 mb-3">Submission windows</h1>

      <div aria-live="polite">
        {error && (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        )}
        {notice && <div className="alert alert-success">{notice}</div>}
      </div>

      {/* ---------- windows ---------- */}
      <div className="tm-card p-4 mb-4">
        <h2 className="h5 mb-2">Report windows</h2>
        <p className="small text-secondary">
          One window per semester, phase and track. Groups can submit between the opening and
          closing time. With a late policy, they may still submit until the late deadline.
        </p>
        <form onSubmit={createWindow} className="row g-3 mb-4">
          <div className="col-sm-4">
            <label htmlFor="w-semester" className="form-label">Semester</label>
            <input
              id="w-semester"
              className="form-control"
              value={semester}
              maxLength={30}
              onChange={(e) => setSemester(e.target.value)}
              required
            />
          </div>
          <div className="col-sm-4">
            <label htmlFor="w-phase" className="form-label">Phase</label>
            <select
              id="w-phase"
              className="form-select"
              value={wPhase}
              onChange={(e) => setWPhase(e.target.value as Phase)}
            >
              {PHASES.map((p) => (
                <option key={p} value={p}>{label(p)}</option>
              ))}
            </select>
          </div>
          <div className="col-sm-4">
            <label htmlFor="w-track" className="form-label">Track</label>
            <select
              id="w-track"
              className="form-select"
              value={wTrack}
              onChange={(e) => setWTrack(e.target.value as Track)}
            >
              {TRACKS.map((t) => (
                <option key={t} value={t}>{label(t)}</option>
              ))}
            </select>
          </div>
          <div className="col-sm-6">
            <label htmlFor="w-opens" className="form-label">Opens</label>
            <input
              id="w-opens"
              type="datetime-local"
              className="form-control"
              value={opensAt}
              onChange={(e) => setOpensAt(e.target.value)}
              required
            />
          </div>
          <div className="col-sm-6">
            <label htmlFor="w-closes" className="form-label">Closes</label>
            <input
              id="w-closes"
              type="datetime-local"
              className="form-control"
              value={closesAt}
              onChange={(e) => setClosesAt(e.target.value)}
              required
            />
          </div>
          <div className="col-sm-6">
            <label htmlFor="w-late" className="form-label">Late policy</label>
            <select
              id="w-late"
              className="form-select"
              value={latePolicy}
              onChange={(e) => setLatePolicy(e.target.value as LatePolicy)}
            >
              <option value="NO_LATE">No late submissions</option>
              <option value="LATE_ALLOWED">Late submissions allowed</option>
            </select>
          </div>
          {latePolicy === 'LATE_ALLOWED' && (
            <div className="col-sm-6">
              <label htmlFor="w-late-until" className="form-label">Late until</label>
              <input
                id="w-late-until"
                type="datetime-local"
                className="form-control"
                value={lateUntil}
                onChange={(e) => setLateUntil(e.target.value)}
                required
              />
            </div>
          )}
          <div className="col-12">
            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? 'Saving...' : 'Create window'}
            </button>
          </div>
        </form>

        {loading ? (
          <p className="text-secondary mb-0">Loading...</p>
        ) : windows.length === 0 ? (
          <p className="text-secondary mb-0">No windows yet.</p>
        ) : (
          <div className="table-responsive">
            <table className="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th scope="col">ID</th>
                  <th scope="col">Semester</th>
                  <th scope="col">Phase</th>
                  <th scope="col">Track</th>
                  <th scope="col">Opens</th>
                  <th scope="col">Closes</th>
                  <th scope="col">Late until</th>
                </tr>
              </thead>
              <tbody>
                {windows.map((w) => (
                  <tr key={w.id}>
                    <td>{w.id}</td>
                    <td>{w.semester}</td>
                    <td>{label(w.phase)}</td>
                    <td>{label(w.track)}</td>
                    <td className="small">{when(w.opensAt)}</td>
                    <td className="small">{when(w.closesAt)}</td>
                    <td className="small">
                      {w.latePolicy === 'LATE_ALLOWED' ? when(w.lateUntil) : 'No late'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* ---------- checklist ---------- */}
      <div className="tm-card p-4 mb-4">
        <h2 className="h5 mb-2">Required deliverables</h2>
        <p className="small text-secondary">
          What each track must hand in at each phase, for example a poster at Pre-Thesis 2.
        </p>
        <form onSubmit={addRequirement} className="row g-3 align-items-end mb-4">
          <div className="col-sm-3">
            <label htmlFor="r-phase" className="form-label">Phase</label>
            <select
              id="r-phase"
              className="form-select"
              value={rPhase}
              onChange={(e) => setRPhase(e.target.value as Phase)}
            >
              {PHASES.map((p) => (
                <option key={p} value={p}>{label(p)}</option>
              ))}
            </select>
          </div>
          <div className="col-sm-3">
            <label htmlFor="r-track" className="form-label">Track</label>
            <select
              id="r-track"
              className="form-select"
              value={rTrack}
              onChange={(e) => setRTrack(e.target.value as Track)}
            >
              {TRACKS.map((t) => (
                <option key={t} value={t}>{label(t)}</option>
              ))}
            </select>
          </div>
          <div className="col-sm-3">
            <label htmlFor="r-type" className="form-label">Deliverable</label>
            <select
              id="r-type"
              className="form-select"
              value={rType}
              onChange={(e) => setRType(e.target.value as DeliverableType)}
            >
              {DELIVERABLE_TYPES.map((d) => (
                <option key={d} value={d}>{label(d)}</option>
              ))}
            </select>
          </div>
          <div className="col-sm-2">
            <div className="form-check">
              <input
                id="r-mandatory"
                type="checkbox"
                className="form-check-input"
                checked={rMandatory}
                onChange={(e) => setRMandatory(e.target.checked)}
              />
              <label htmlFor="r-mandatory" className="form-check-label">Mandatory</label>
            </div>
          </div>
          <div className="col-sm-1">
            <button type="submit" className="btn btn-primary w-100" disabled={busy}>
              Add
            </button>
          </div>
        </form>

        {requirements.length === 0 ? (
          <p className="text-secondary mb-0">No deliverables on the checklist yet.</p>
        ) : (
          <div className="table-responsive">
            <table className="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th scope="col">Phase</th>
                  <th scope="col">Track</th>
                  <th scope="col">Deliverable</th>
                  <th scope="col">Mandatory</th>
                  <th scope="col"><span className="visually-hidden">Actions</span></th>
                </tr>
              </thead>
              <tbody>
                {requirements.map((r) => (
                  <tr key={r.id}>
                    <td>{label(r.phase)}</td>
                    <td>{label(r.track)}</td>
                    <td>{label(r.deliverableType)}</td>
                    <td>{r.mandatory ? 'Yes' : 'No'}</td>
                    <td className="text-end">
                      <button
                        type="button"
                        className="btn btn-outline-danger btn-sm"
                        disabled={busy}
                        onClick={() => removeRequirement(r.id)}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* ---------- extensions ---------- */}
      <div className="tm-card p-4">
        <h2 className="h5 mb-2">Extensions</h2>
        <p className="small text-secondary">
          Give one group a later closing time for one window. A reason is required and the new
          time must be after the window closes.
        </p>
        <form onSubmit={grantExtension} className="row g-3 mb-4">
          <div className="col-sm-4">
            <label htmlFor="e-window" className="form-label">Window</label>
            <select
              id="e-window"
              className="form-select"
              value={extWindowId}
              onChange={(e) => setExtWindowId(e.target.value)}
              required
            >
              <option value="">Choose a window</option>
              {windows.map((w) => (
                <option key={w.id} value={w.id}>
                  #{w.id} {w.semester} {label(w.phase)} {label(w.track)}
                </option>
              ))}
            </select>
          </div>
          <div className="col-sm-3">
            <label htmlFor="e-group" className="form-label">Group ID</label>
            <input
              id="e-group"
              type="number"
              min={1}
              className="form-control"
              value={extGroupId}
              onChange={(e) => setExtGroupId(e.target.value)}
              required
            />
          </div>
          <div className="col-sm-5">
            <label htmlFor="e-closes" className="form-label">New closing time</label>
            <input
              id="e-closes"
              type="datetime-local"
              className="form-control"
              value={extClosesAt}
              onChange={(e) => setExtClosesAt(e.target.value)}
              required
            />
          </div>
          <div className="col-12">
            <label htmlFor="e-reason" className="form-label">Reason</label>
            <input
              id="e-reason"
              className="form-control"
              value={extReason}
              maxLength={500}
              onChange={(e) => setExtReason(e.target.value)}
              required
            />
          </div>
          <div className="col-12">
            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? 'Saving...' : 'Save extension'}
            </button>
          </div>
        </form>

        {!extWindowId ? (
          <p className="text-secondary mb-0">Choose a window to see its extensions.</p>
        ) : extensions.length === 0 ? (
          <p className="text-secondary mb-0">No extensions for this window.</p>
        ) : (
          <div className="table-responsive">
            <table className="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th scope="col">Group</th>
                  <th scope="col">New closing time</th>
                  <th scope="col">Reason</th>
                  <th scope="col">Granted by</th>
                </tr>
              </thead>
              <tbody>
                {extensions.map((x) => (
                  <tr key={x.id}>
                    <td>{x.groupId}</td>
                    <td className="small">{when(x.newClosesAt)}</td>
                    <td>{x.reason}</td>
                    <td className="small">{x.grantedByName}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
