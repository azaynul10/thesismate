import { useCallback, useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import { TRACK_LABELS } from '../api/groups';
import { MilestonePlan, milestonesApi } from '../api/milestones';
import { GroupProgress, WeeklyOverview, WeeklyReport, weeklyReportsApi } from '../api/weeklyReports';
import MilestoneBoard from '../components/MilestoneBoard';
import ProgressBar from '../components/ProgressBar';
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

export default function SupervisorProgressPage({ user, authChecked }: Props) {
  const isSupervisor = user?.role === 'SUPERVISOR';
  const [groups, setGroups] = useState<GroupProgress[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [selectedId, setSelectedId] = useState<number | ''>('');
  const [overview, setOverview] = useState<WeeklyOverview | null>(null);
  const [plan, setPlan] = useState<MilestonePlan | null>(null);
  const [openReport, setOpenReport] = useState<WeeklyReport | null>(null);
  const [remark, setRemark] = useState('');

  const loadGroups = useCallback(async () => {
    try {
      setGroups(await weeklyReportsApi.progress());
    } catch (e) {
      setError(messageOf(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isSupervisor) void loadGroups();
  }, [isSupervisor, loadGroups]);

  useEffect(() => {
    if (!openReport) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpenReport(null);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [openReport]);

  async function loadReports(groupId: number) {
    try {
      setOverview(await weeklyReportsApi.overview(groupId));
    } catch (e) {
      setError(messageOf(e));
    }
  }

  async function choose(value: string) {
    setError(null);
    setPlan(null);
    setOverview(null);
    if (value === '') {
      setSelectedId('');
      return;
    }
    const id = Number(value);
    setSelectedId(id);
    await loadReports(id);
  }

  async function showPlan(groupId: number) {
    setError(null);
    try {
      setPlan(await milestonesApi.plan(groupId));
    } catch (e) {
      setError(messageOf(e));
    }
  }

  function open(report: WeeklyReport) {
    setRemark('');
    setError(null);
    setOpenReport(report);
  }

  async function review(approve: boolean) {
    if (!openReport || selectedId === '') return;
    const text = remark.trim();
    if (!approve && text === '') {
      setError('Write your remark before sending it back to the group.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      if (approve) {
        await weeklyReportsApi.approve(openReport.id, text || undefined);
      } else {
        await weeklyReportsApi.returnReport(openReport.id, text);
      }
      setOpenReport(null);
      await Promise.all([loadReports(selectedId), loadGroups()]);
    } catch (e) {
      setError(messageOf(e));
    } finally {
      setBusy(false);
    }
  }

  if (!authChecked) return <p className="text-secondary">Loading...</p>;
  if (!user) return <Navigate to="/login" replace />;
  if (!isSupervisor) {
    return (
      <div className="tm-card p-4">
        <h1 className="h4 mb-2">Progress</h1>
        <p className="text-secondary mb-0">This page is for supervisors. Your role is {user.role}.</p>
      </div>
    );
  }
  if (loading) return <p className="text-secondary">Loading...</p>;

  const selected = groups.find((g) => g.groupId === selectedId) ?? null;

  return (
    <div>
      <h1 className="h3 mb-3">Group progress</h1>

      <div aria-live="polite">
        {error && (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        )}
      </div>

      {groups.length === 0 ? (
        <p className="text-secondary">You have no allocated groups yet.</p>
      ) : (
        <>
          <div className="mb-3">
            <label htmlFor="sp-group" className="form-label">
              Group
            </label>
            <select
              id="sp-group"
              className="form-select"
              value={selectedId}
              onChange={(e) => void choose(e.target.value)}
            >
              <option value="">Select a group</option>
              {groups.map((g) => (
                <option key={g.groupId} value={g.groupId}>
                  {g.memberNames.join(', ')} ({TRACK_LABELS[g.track]}, {g.semester})
                  {g.pendingReports > 0 ? ` - ${g.pendingReports} waiting` : ''}
                </option>
              ))}
            </select>
          </div>

          {!selected && (
            <div className="row g-2">
              {groups.map((g) => (
                <div key={g.groupId} className="col-12">
                  <button
                    type="button"
                    className="tm-card p-3 w-100 text-start border-0"
                    onClick={() => void choose(String(g.groupId))}
                  >
                    <div className="d-flex flex-wrap justify-content-between gap-2">
                      <strong>{g.memberNames.join(', ')}</strong>
                      <span className="small text-secondary">
                        {TRACK_LABELS[g.track]} · {g.progress.completionPercent}% complete
                        {g.pendingReports > 0 && ` · ${g.pendingReports} report(s) waiting`}
                      </span>
                    </div>
                  </button>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {selected && (
        <div>
          <div className="tm-card p-3 mb-3">
            <div className="d-flex flex-wrap justify-content-between gap-2 mb-2">
              <div>
                <strong>{selected.memberNames.join(', ')}</strong>
                <div className="small text-secondary">
                  {TRACK_LABELS[selected.track]} · {selected.semester} ·{' '}
                  {selected.myRole === 'SUPERVISOR' ? 'Supervisor' : 'Co-supervisor'}
                </div>
              </div>
              <button
                type="button"
                className="btn btn-outline-primary btn-sm align-self-start"
                onClick={() => void showPlan(selected.groupId)}
              >
                View plan
              </button>
            </div>
            <ProgressBar percent={selected.progress.completionPercent} label="Completion" />
            <div className="small text-secondary mt-2">
              {selected.progress.doneTasks} of {selected.progress.totalTasks} tasks done
              {selected.progress.overdueTasks > 0 && (
                <span className="text-danger"> · {selected.progress.overdueTasks} overdue</span>
              )}
              {selected.weeklyReportsApplicable ? (
                <>
                  {' · '}
                  {selected.missingWeeks > 0 ? (
                    <span className="text-danger">{selected.missingWeeks} week(s) without a report</span>
                  ) : (
                    'every week has a report'
                  )}
                </>
              ) : (
                ' · no weekly reports for this track'
              )}
            </div>
          </div>

          {plan && plan.groupId === selected.groupId && (
            <div className="mb-4">
              <div className="d-flex justify-content-between align-items-center mb-2">
                <h2 className="h5 mb-0">Plan, read-only</h2>
                <button type="button" className="btn btn-link btn-sm" onClick={() => setPlan(null)}>
                  Close
                </button>
              </div>
              <MilestoneBoard plan={plan} members={[]} onChanged={() => undefined} onError={setError} />
            </div>
          )}

          <h2 className="h5 mb-2">Weekly reports</h2>
          {!overview ? (
            <p className="text-secondary">Loading...</p>
          ) : overview.reports.length === 0 ? (
            <p className="text-secondary">This group has not sent any report yet.</p>
          ) : (
            <ul className="list-group mb-3">
              {overview.reports.map((r) => (
                <li key={r.id} className="list-group-item list-group-item-action p-0">
                  <button
                    type="button"
                    className="btn w-100 text-start p-3 border-0"
                    onClick={() => open(r)}
                    aria-label={`Open report of week ${day(r.weekStart)} sent ${when(r.submittedAt)}`}
                  >
                    <div className="d-flex flex-wrap justify-content-between align-items-center gap-2">
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
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {openReport && (
        <>
          <div className="modal-backdrop fade show" />
          <div
            className="modal fade show d-block"
            role="dialog"
            aria-modal="true"
            aria-labelledby="report-dialog-title"
            tabIndex={-1}
            onClick={(e) => {
              if (e.target === e.currentTarget) setOpenReport(null);
            }}
          >
            <div className="modal-dialog modal-lg modal-dialog-scrollable">
              <div className="modal-content">
                <div className="modal-header">
                  <h2 className="modal-title h5" id="report-dialog-title">
                    Report of week {day(openReport.weekStart)}
                  </h2>
                  <button type="button" className="btn-close" aria-label="Close" onClick={() => setOpenReport(null)} />
                </div>
                <div className="modal-body">
                  <p className="small text-secondary">
                    {openReport.memberNames.join(', ')} · sent by {openReport.submittedByName} on{' '}
                    {when(openReport.submittedAt)}
                    {openReport.late && ' · late'}
                  </p>
                  <p style={{ whiteSpace: 'pre-wrap' }}><strong>Done:</strong> {openReport.done}</p>
                  <p style={{ whiteSpace: 'pre-wrap' }}><strong>Next:</strong> {openReport.next}</p>
                  {openReport.blockers && (
                    <p style={{ whiteSpace: 'pre-wrap' }}><strong>Blockers:</strong> {openReport.blockers}</p>
                  )}

                  {openReport.status === 'SUBMITTED' ? (
                    <>
                      <label htmlFor="report-remark" className="form-label">
                        Your remark <span className="text-secondary">(needed when you send the report back)</span>
                      </label>
                      <textarea
                        id="report-remark"
                        className="form-control"
                        rows={3}
                        value={remark}
                        onChange={(e) => setRemark(e.target.value)}
                      />
                    </>
                  ) : (
                    <div className="alert alert-light border mb-0" style={{ whiteSpace: 'pre-wrap' }}>
                      <span className={`badge ${STATUS_BADGE[openReport.status]} me-2`}>
                        {STATUS_LABEL[openReport.status]}
                      </span>
                      {openReport.reviewedByName}
                      {openReport.reviewedAt ? ` (${when(openReport.reviewedAt)})` : ''}:{' '}
                      {openReport.reviewComment ?? 'No remark'}
                    </div>
                  )}
                </div>
                <div className="modal-footer">
                  <button type="button" className="btn btn-link" onClick={() => setOpenReport(null)}>
                    Close
                  </button>
                  {openReport.status === 'SUBMITTED' && (
                    <>
                      <button
                        type="button"
                        className="btn btn-outline-danger"
                        disabled={busy}
                        onClick={() => void review(false)}
                      >
                        Send remark
                      </button>
                      <button
                        type="button"
                        className="btn btn-success"
                        disabled={busy}
                        onClick={() => void review(true)}
                      >
                        Approve
                      </button>
                    </>
                  )}
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
