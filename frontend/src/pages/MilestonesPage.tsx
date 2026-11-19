import { useCallback, useEffect, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import { Group, TRACK_LABELS, groupsApi } from '../api/groups';
import { MilestonePlan, milestonesApi } from '../api/milestones';
import MilestoneBoard from '../components/MilestoneBoard';
import ProgressBar from '../components/ProgressBar';
import { AuthUser } from './LoginPage';

interface Props {
  user: AuthUser | null;
  authChecked: boolean;
}

function messageOf(e: unknown): string {
  return e instanceof ApiError ? e.message : 'Something went wrong. Please try again.';
}

export default function MilestonesPage({ user, authChecked }: Props) {
  const isStudent = user?.role === 'STUDENT';
  const [group, setGroup] = useState<Group | null>(null);
  const [plan, setPlan] = useState<MilestonePlan | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const g = await groupsApi.myGroup();
      setGroup(g);
      setPlan(await milestonesApi.plan(g.id));
      setError(null);
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

  async function useTemplate() {
    if (!group) return;
    setBusy(true);
    setError(null);
    try {
      setPlan(await milestonesApi.applyTemplate(group.id));
    } catch (e) {
      setError(messageOf(e));
    } finally {
      setBusy(false);
    }
  }

  if (!authChecked) return <p className="text-secondary">Loading...</p>;
  if (!user) return <Navigate to="/login" replace />;
  if (!isStudent) {
    return (
      <div className="tm-card p-4">
        <h1 className="h4 mb-2">Milestones</h1>
        <p className="text-secondary mb-0">
          Milestones are planned by student groups. Supervisors can open a group plan from the Progress page.
        </p>
      </div>
    );
  }
  if (loading) return <p className="text-secondary">Loading...</p>;
  if (!group || !plan) {
    return (
      <div className="tm-card p-4">
        <h1 className="h4 mb-2">Milestones</h1>
        <p className="mb-0">
          You need a group before you can plan milestones. <Link to="/groups">Go to My Group</Link>.
        </p>
      </div>
    );
  }

  return (
    <div>
      <h1 className="h3 mb-1">Milestones and tasks</h1>
      <p className="text-secondary">
        {TRACK_LABELS[plan.track]} group, {plan.semester}
      </p>

      <div aria-live="polite">
        {error && (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        )}
      </div>

      {!plan.canEdit && (
        <div className="alert alert-secondary">This group is no longer active, so the plan is read-only.</div>
      )}

      <div className="tm-card p-3 mb-3">
        <div className="d-flex flex-wrap justify-content-between gap-2 mb-2">
          <strong>Overall completion</strong>
          <span className="small text-secondary">
            {plan.summary.doneTasks} of {plan.summary.totalTasks} tasks done
            {' · '}
            {plan.summary.completedMilestones} of {plan.summary.totalMilestones} milestones completed
            {plan.summary.overdueTasks > 0 && (
              <span className="text-danger"> · {plan.summary.overdueTasks} overdue</span>
            )}
          </span>
        </div>
        <ProgressBar percent={plan.summary.completionPercent} label="Overall completion" />
      </div>

      {plan.canEdit && !plan.templateApplied && (
        <div className="tm-card p-3 mb-3 d-flex flex-wrap justify-content-between align-items-center gap-2">
          <span>
            Start from the {TRACK_LABELS[plan.track].toLowerCase()} milestone template. Due dates follow your
            submission windows.
          </span>
          <button type="button" className="btn btn-primary" disabled={busy} onClick={() => void useTemplate()}>
            Use template
          </button>
        </div>
      )}

      <MilestoneBoard
        plan={plan}
        members={group.members}
        onChanged={load}
        onError={setError}
      />
    </div>
  );
}
