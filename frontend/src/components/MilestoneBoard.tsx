import { FormEvent, useState } from 'react';
import { ApiError } from '../api/client';
import { GroupMemberInfo } from '../api/groups';
import {
  MilestonePlan,
  MilestoneView,
  TASK_STATUS_LABELS,
  TaskStatus,
  TaskView,
  milestonesApi,
} from '../api/milestones';
import ProgressBar from './ProgressBar';

interface Props {
  plan: MilestonePlan;
  members: GroupMemberInfo[];
  onChanged: () => Promise<void> | void;
  onError: (message: string) => void;
}

function messageOf(e: unknown): string {
  if (e instanceof ApiError) {
    const details = Object.values(e.fieldErrors);
    return details.length > 0 ? `${e.message}: ${details.join(', ')}` : e.message;
  }
  return 'Something went wrong. Please try again.';
}

function shortDate(value: string | null): string {
  if (!value) return 'No date';
  return new Date(`${value}T00:00:00`).toLocaleDateString(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

const STATE_BADGE: Record<MilestoneView['state'], string> = {
  NOT_STARTED: 'text-bg-light border',
  IN_PROGRESS: 'text-bg-info',
  COMPLETED: 'text-bg-success',
};

const STATE_LABEL: Record<MilestoneView['state'], string> = {
  NOT_STARTED: 'Not started',
  IN_PROGRESS: 'In progress',
  COMPLETED: 'Completed',
};

/** Shows a group's milestones and tasks. With plan.canEdit the group can change them, otherwise it is read-only. */
export default function MilestoneBoard({ plan, members, onChanged, onError }: Props) {
  const [busy, setBusy] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [newDue, setNewDue] = useState('');
  const [taskFor, setTaskFor] = useState<number | null>(null);
  const [taskTitle, setTaskTitle] = useState('');
  const [taskAssignee, setTaskAssignee] = useState('');
  const [taskDue, setTaskDue] = useState('');

  async function run(action: () => Promise<unknown>): Promise<boolean> {
    setBusy(true);
    try {
      await action();
      await onChanged();
      return true;
    } catch (e) {
      onError(messageOf(e));
      return false;
    } finally {
      setBusy(false);
    }
  }

  async function addMilestone(e: FormEvent) {
    e.preventDefault();
    const ok = await run(() =>
      milestonesApi.addMilestone(plan.groupId, { title: newTitle, dueDate: newDue || null }),
    );
    if (ok) {
      setNewTitle('');
      setNewDue('');
    }
  }

  async function addTask(e: FormEvent, milestoneId: number) {
    e.preventDefault();
    const ok = await run(() =>
      milestonesApi.addTask(milestoneId, {
        title: taskTitle,
        assigneeId: taskAssignee ? Number(taskAssignee) : null,
        dueDate: taskDue || null,
      }),
    );
    if (ok) {
      setTaskFor(null);
      setTaskTitle('');
      setTaskAssignee('');
      setTaskDue('');
    }
  }

  function removeMilestone(m: MilestoneView) {
    if (window.confirm(`Delete "${m.title}" and its ${m.totalTasks} task(s)?`)) {
      void run(() => milestonesApi.deleteMilestone(m.id));
    }
  }

  function removeTask(t: TaskView) {
    if (window.confirm(`Delete task "${t.title}"?`)) {
      void run(() => milestonesApi.deleteTask(t.id));
    }
  }

  function reassign(t: TaskView, value: string) {
    void run(() =>
      milestonesApi.updateTask(t.id, {
        title: t.title,
        description: t.description ?? undefined,
        assigneeId: value ? Number(value) : null,
        dueDate: t.dueDate,
      }),
    );
  }

  return (
    <div>
      {plan.milestones.length === 0 && (
        <p className="text-secondary">No milestones yet.</p>
      )}

      {plan.milestones.map((m) => (
        <section key={m.id} className="tm-card p-3 mb-3" aria-label={`Milestone ${m.title}`}>
          <div className="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-2">
            <div>
              <h2 className="h6 mb-1">{m.title}</h2>
              {m.description && <p className="small text-secondary mb-1">{m.description}</p>}
              <div className="small text-secondary">
                Due {shortDate(m.dueDate)}
                {m.phase && <> · {m.phase.replace(/_/g, ' ')}</>}
              </div>
            </div>
            <div className="d-flex align-items-center gap-2">
              {m.overdue && <span className="badge text-bg-danger">Overdue</span>}
              <span className={`badge ${STATE_BADGE[m.state]}`}>{STATE_LABEL[m.state]}</span>
              {plan.canEdit && (
                <button
                  type="button"
                  className="btn btn-outline-danger btn-sm"
                  disabled={busy}
                  onClick={() => removeMilestone(m)}
                >
                  Delete
                </button>
              )}
            </div>
          </div>

          <ProgressBar percent={m.completionPercent} label={`${m.title} completion`} />

          {m.tasks.length > 0 && (
            <ul className="list-group list-group-flush mt-2">
              {m.tasks.map((t) => (
                <li key={t.id} className="list-group-item px-0">
                  <div className="d-flex flex-wrap justify-content-between align-items-center gap-2">
                    <div>
                      <span className={t.status === 'DONE' ? 'text-decoration-line-through text-secondary' : ''}>
                        {t.title}
                      </span>
                      {t.overdue && <span className="badge text-bg-danger ms-2">Overdue</span>}
                      <div className="small text-secondary">
                        {t.assigneeName ?? 'Unassigned'}
                        {t.assigneeName && !t.assigneeActive && ' (left the group)'}
                        {' · '}due {shortDate(t.dueDate)}
                      </div>
                    </div>
                    <div className="d-flex align-items-center gap-2">
                      {plan.canEdit ? (
                        <>
                          <select
                            className="form-select form-select-sm"
                            aria-label={`Assignee of ${t.title}`}
                            value={t.assigneeActive && t.assigneeId ? String(t.assigneeId) : ''}
                            disabled={busy}
                            onChange={(e) => reassign(t, e.target.value)}
                          >
                            <option value="">Unassigned</option>
                            {members.map((mem) => (
                              <option key={mem.userId} value={mem.userId}>
                                {mem.fullName}
                              </option>
                            ))}
                          </select>
                          <select
                            className="form-select form-select-sm"
                            aria-label={`Status of ${t.title}`}
                            value={t.status}
                            disabled={busy}
                            onChange={(e) =>
                              void run(() => milestonesApi.setStatus(t.id, e.target.value as TaskStatus))
                            }
                          >
                            {(Object.keys(TASK_STATUS_LABELS) as TaskStatus[]).map((s) => (
                              <option key={s} value={s}>
                                {TASK_STATUS_LABELS[s]}
                              </option>
                            ))}
                          </select>
                          <button
                            type="button"
                            className="btn btn-outline-secondary btn-sm"
                            aria-label={`Delete task ${t.title}`}
                            disabled={busy}
                            onClick={() => removeTask(t)}
                          >
                            Remove
                          </button>
                        </>
                      ) : (
                        <span className="badge text-bg-light border">{TASK_STATUS_LABELS[t.status]}</span>
                      )}
                    </div>
                  </div>
                </li>
              ))}
            </ul>
          )}

          {plan.canEdit &&
            (taskFor === m.id ? (
              <form className="row g-2 mt-2" onSubmit={(e) => void addTask(e, m.id)}>
                <div className="col-12 col-md-5">
                  <input
                    className="form-control form-control-sm"
                    placeholder="Task title"
                    aria-label="Task title"
                    value={taskTitle}
                    onChange={(e) => setTaskTitle(e.target.value)}
                    required
                  />
                </div>
                <div className="col-6 col-md-3">
                  <select
                    className="form-select form-select-sm"
                    aria-label="Assignee"
                    value={taskAssignee}
                    onChange={(e) => setTaskAssignee(e.target.value)}
                  >
                    <option value="">Unassigned</option>
                    {members.map((mem) => (
                      <option key={mem.userId} value={mem.userId}>
                        {mem.fullName}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="col-6 col-md-2">
                  <input
                    type="date"
                    className="form-control form-control-sm"
                    aria-label="Task due date"
                    value={taskDue}
                    onChange={(e) => setTaskDue(e.target.value)}
                  />
                </div>
                <div className="col-12 col-md-2 d-flex gap-2">
                  <button type="submit" className="btn btn-primary btn-sm" disabled={busy}>
                    Add
                  </button>
                  <button type="button" className="btn btn-link btn-sm" onClick={() => setTaskFor(null)}>
                    Cancel
                  </button>
                </div>
              </form>
            ) : (
              <button
                type="button"
                className="btn btn-outline-primary btn-sm mt-2"
                onClick={() => setTaskFor(m.id)}
              >
                Add task
              </button>
            ))}
        </section>
      ))}

      {plan.canEdit && (
        <form className="tm-card p-3" onSubmit={(e) => void addMilestone(e)}>
          <h2 className="h6 mb-2">Add a milestone</h2>
          <div className="row g-2">
            <div className="col-12 col-md-7">
              <input
                className="form-control"
                placeholder="Milestone title"
                aria-label="Milestone title"
                value={newTitle}
                onChange={(e) => setNewTitle(e.target.value)}
                required
              />
            </div>
            <div className="col-6 col-md-3">
              <input
                type="date"
                className="form-control"
                aria-label="Milestone due date"
                value={newDue}
                onChange={(e) => setNewDue(e.target.value)}
              />
            </div>
            <div className="col-6 col-md-2">
              <button type="submit" className="btn btn-primary w-100" disabled={busy}>
                Add
              </button>
            </div>
          </div>
        </form>
      )}
    </div>
  );
}
