import { FormEvent, useCallback, useEffect, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import { Group, groupsApi } from '../api/groups';
import { WeeklyOverview, WeeklyReport, weeklyReportsApi } from '../api/weeklyReports';
import { AuthUser } from './LoginPage';

interface Props {
  user: AuthUser | null;
  authChecked: boolean;
}

const ALL_WEEKS = 'ALL';

function messageOf(e: unknown): string {
  if (e instanceof ApiError) {
    const details = Object.values(e.fieldErrors);
    return details.length > 0 ? `${e.message}: ${details.join(', ')}` : e.message;
  }
  return 'Something went wrong. Please try again.';
}

function day(value: string): string {
  return new Date(`${value}T00:00:00`).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
}

function when(value: string): string {
  return new Date(value).toLocaleString();
}

const STATUS_BADGE: Record<WeeklyReport['status'], string> = {
  SUBMITTED: 'text-bg-warning',
  APPROVED: 'text-bg-success',
  RETURNED: 'text-bg-danger',
};

const STATUS_LABEL: Record<WeeklyReport['status'], string> = {
  SUBMITTED: 'Waiting for sign-off',
  APPROVED: 'Approved',
  RETURNED: 'Returned with a remark',
};

export default function WeeklyReportsPage({ user, authChecked }: Props) {
  const isStudent = user?.role === 'STUDENT';
  const [group, setGroup] = useState<Group | null>(null);
  const [overview, setOverview] = useState<WeeklyOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  // form: which week the new report is for
  const [week, setWeek] = useState('');
  const [done, setDone] = useState('');
  const [next, setNext] = useState('');
  const [blockers, setBlockers] = useState('');

  // history: which week's reports are shown
  const [viewWeek, setViewWeek] = useState<string>(ALL_WEEKS);

  const load = useCallback(async () => {
    try {
      const g = await groupsApi.myGroup();
      setGroup(g);
      const o = await weeklyReportsApi.overview(g.id);
      setOverview(o);
      setWeek((w) => w || o.currentWeek);
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) {
        setGroup(null);
      } else {
        setError(messageOf(e));
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isStudent) void load();
  }, [isStudent, load]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!group) return;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await weeklyReportsApi.submit(group.id, week, { done, next, blockers });
      setNotice(`Report for the week of ${day(week)} sent to your supervisor.`);
      setDone('');
      setNext('');
      setBlockers('');
      setViewWeek(week);
      await load();
    } catch (err) {
      setError(messageOf(err));
    } finally {
      setBusy(false);
    }
  }

  if (!authChecked) return <p className="text-secondary">Loading...</p>;
  if (!user) return <Navigate to="/login" replace />;
  if (!isStudent) {
    return (
      <div className="tm-card p-4">
        <h1 className="h4 mb-2">Weekly reports</h1>
        <p className="text-secondary mb-0">
          Weekly reports are handed in by student groups. Supervisors review them on the Progress page.
        </p>
      </div>
    );
  }
  if (loading) return <p className="text-secondary">Loading...</p>;
  if (!group || !overview) {
    return (
      <div className="tm-card p-4">
        <h1 className="h4 mb-2">Weekly reports</h1>
        <p className="mb-0">
          You need a group before you can hand in weekly reports. <Link to="/groups">Go to My Group</Link>.
        </p>
      </div>
    );
  }

  const reportWeeks = Array.from(new Set(overview.reports.map((r) => r.weekStart))).sort().reverse();
  const formWeeks = Array.from(new Set([overview.currentWeek, ...overview.missingWeeks, ...reportWeeks]))
    .sort()
    .reverse();
  const shown =
    viewWeek === ALL_WEEKS ? overview.reports : overview.reports.filter((r) => r.weekStart === viewWeek);
  const countIn = (w: string) => overview.reports.filter((r) => r.weekStart === w).length;

  return (
    <div>
      <h1 className="h3 mb-3">Weekly reports</h1>

      <div aria-live="polite">
        {error && (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        )}
        {notice && (
          <div className="alert alert-success" role="status">
            {notice}
          </div>
        )}
      </div>

      {overview.notice && <div className="alert alert-secondary">{overview.notice}</div>}

      {overview.applicable && overview.missingWeeks.length > 0 && (
        <div className="alert alert-warning">
          No report yet for: {overview.missingWeeks.map((w) => `week of ${day(w)}`).join(', ')}.
        </div>
      )}

      {overview.canSubmit && (
        <form className="tm-card p-3 mb-4" onSubmit={(e) => void submit(e)}>
          <h2 className="h5 mb-3">Send a report</h2>
          <p className="small text-secondary">
            You can send more than one report for the same week. Every report is kept.
          </p>
          <div className="mb-3">
            <label htmlFor="wr-week" className="form-label">
              Report is for the week of
            </label>
            <select id="wr-week" className="form-select" value={week} onChange={(e) => setWeek(e.target.value)}>
              {formWeeks.map((w) => (
                <option key={w} value={w}>
                  {day(w)}
                  {w === overview.currentWeek ? ' (this week)' : ''}
                </option>
              ))}
            </select>
          </div>
          <div className="mb-3">
            <label htmlFor="wr-done" className="form-label">
              What was done
            </label>
            <textarea id="wr-done" className="form-control" rows={3} value={done} onChange={(e) => setDone(e.target.value)} required />
          </div>
          <div className="mb-3">
            <label htmlFor="wr-next" className="form-label">
              What comes next
            </label>
            <textarea id="wr-next" className="form-control" rows={3} value={next} onChange={(e) => setNext(e.target.value)} required />
          </div>
          <div className="mb-3">
            <label htmlFor="wr-blockers" className="form-label">
              Blockers <span className="text-secondary">(optional)</span>
            </label>
            <textarea id="wr-blockers" className="form-control" rows={2} value={blockers} onChange={(e) => setBlockers(e.target.value)} />
          </div>
          <button type="submit" className="btn btn-primary" disabled={busy}>
            Submit report
          </button>
        </form>
      )}

      <section aria-label="Sent reports">
        <div className="d-flex flex-wrap justify-content-between align-items-end gap-2 mb-3">
          <h2 className="h5 mb-0">Reports you have sent</h2>
          <div>
            <label htmlFor="wr-view" className="form-label small mb-1">
              Show week
            </label>
            <select
              id="wr-view"
              className="form-select form-select-sm"
              value={viewWeek}
              onChange={(e) => setViewWeek(e.target.value)}
            >
              <option value={ALL_WEEKS}>All weeks ({overview.reports.length})</option>
              {reportWeeks.map((w) => (
                <option key={w} value={w}>
                  Week of {day(w)} ({countIn(w)})
                </option>
              ))}
            </select>
          </div>
        </div>

        {shown.length === 0 ? (
          <p className="text-secondary">No reports for this selection.</p>
        ) : (
          shown.map((r) => (
            <article key={r.id} className="tm-card p-3 mb-2" aria-label={`Report sent ${when(r.submittedAt)}`}>
              <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
                <div>
                  <strong>Week of {day(r.weekStart)}</strong>
                  <div className="small text-secondary">
                    Sent by {r.submittedByName} on {when(r.submittedAt)}
                  </div>
                </div>
                <div className="d-flex gap-2">
                  {r.late && <span className="badge text-bg-secondary">Late</span>}
                  <span className={`badge ${STATUS_BADGE[r.status]}`}>{STATUS_LABEL[r.status]}</span>
                </div>
              </div>
              <p className="mb-1" style={{ whiteSpace: 'pre-wrap' }}><strong>Done:</strong> {r.done}</p>
              <p className="mb-1" style={{ whiteSpace: 'pre-wrap' }}><strong>Next:</strong> {r.next}</p>
              {r.blockers && <p className="mb-1" style={{ whiteSpace: 'pre-wrap' }}><strong>Blockers:</strong> {r.blockers}</p>}
              {r.reviewedByName && (
                <p className="mb-0 small text-secondary" style={{ whiteSpace: 'pre-wrap' }}>
                  {r.reviewedByName}
                  {r.reviewedAt ? ` (${when(r.reviewedAt)})` : ''}: {r.reviewComment ?? 'No remark'}
                </p>
              )}
            </article>
          ))
        )}
      </section>
    </div>
  );
}
