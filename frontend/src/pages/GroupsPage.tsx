import { FormEvent, useCallback, useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import { Group, GroupInvitation, GroupMemberInfo, TRACK_LABELS, Track, groupsApi } from '../api/groups';
import { AuthUser } from './LoginPage';

interface Props {
  user: AuthUser | null;
  authChecked: boolean;
}

function messageOf(e: unknown): string {
  return e instanceof ApiError ? e.message : 'Something went wrong. Please try again.';
}

export default function GroupsPage({ user, authChecked }: Props) {
  const [group, setGroup] = useState<Group | null>(null);
  const [invitations, setInvitations] = useState<GroupInvitation[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [track, setTrack] = useState<Track>('THESIS');
  const [semester, setSemester] = useState('Spring 2027');
  const [inviteId, setInviteId] = useState('');

  const isStudent = user?.role === 'STUDENT';

  const load = useCallback(async () => {
    try {
      const [g, inv] = await Promise.all([
        groupsApi.myGroup().catch((e: unknown) => {
          if (e instanceof ApiError && e.status === 404) return null;
          throw e;
        }),
        groupsApi.myInvitations(),
      ]);
      setGroup(g);
      setInvitations(inv);
    } catch (e) {
      setError(messageOf(e));
    } finally {
      setLoading(false);
    }
  }, []);

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

  function handleCreate(e: FormEvent) {
    e.preventDefault();
    void run(() => groupsApi.create(track, semester.trim()), 'Group created.');
  }

  async function handleInvite(e: FormEvent) {
    e.preventDefault();
    if (!group) return;
    const studentId = inviteId.trim();
    const ok = await run(() => groupsApi.invite(group.id, studentId), `Invitation sent to ${studentId}.`);
    if (ok) setInviteId('');
  }

  function handleRespond(invitation: GroupInvitation, accept: boolean) {
    void run(
      () => groupsApi.respond(invitation.id, accept),
      accept ? 'You joined the group.' : 'Invitation declined.',
    );
  }

  function handleTransfer(member: GroupMemberInfo) {
    if (!group || !member.studentId) return;
    const studentId = member.studentId;
    const sure = window.confirm(
      `Make ${member.fullName} the group leader? You will no longer be able to invite members.`,
    );
    if (sure) {
      void run(() => groupsApi.transferLeader(group.id, studentId), `${member.fullName} is now the leader.`);
    }
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
        <h1 className="h4 mb-2">My group</h1>
        <p className="text-secondary mb-0">Groups are for students. Your role is {user.role}.</p>
      </div>
    );
  }

  const isLeader = group !== null && group.leaderId === user.id;
  const maxSize = group?.track === 'INTERNSHIP' ? 1 : 5;
  const canInvite = group !== null && isLeader && group.members.length < maxSize;

  return (
    <div>
      <h1 className="h3 mb-3">My group</h1>

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
      ) : group === null ? (
        <>
          <div className="tm-card p-4 mb-4">
            <h2 className="h5 mb-3">Create a group</h2>
            <p className="small text-secondary">
              Your credits and CGPA are checked against the records the coordinator imported.
              Thesis needs a CGPA of at least 3.00, Project and Internship at least 2.00, and
              everyone needs at least 75 completed credits. Internship groups have exactly one
              member.
            </p>
            <form onSubmit={handleCreate} className="row g-3 align-items-end">
              <div className="col-sm-4">
                <label htmlFor="group-track" className="form-label">Track</label>
                <select
                  id="group-track"
                  className="form-select"
                  value={track}
                  onChange={(e) => setTrack(e.target.value as Track)}
                >
                  {(Object.keys(TRACK_LABELS) as Track[]).map((t) => (
                    <option key={t} value={t}>
                      {TRACK_LABELS[t]}
                    </option>
                  ))}
                </select>
              </div>
              <div className="col-sm-4">
                <label htmlFor="group-semester" className="form-label">Semester</label>
                <input
                  id="group-semester"
                  className="form-control"
                  value={semester}
                  onChange={(e) => setSemester(e.target.value)}
                  maxLength={30}
                  required
                />
              </div>
              <div className="col-sm-4">
                <button type="submit" className="btn btn-primary w-100" disabled={busy}>
                  Create group
                </button>
              </div>
            </form>
          </div>

          <div className="tm-card p-4">
            <h2 className="h5 mb-3">Invitations for you</h2>
            {invitations.length === 0 ? (
              <p className="text-secondary mb-0">No pending invitations.</p>
            ) : (
              <ul className="list-group list-group-flush">
                {invitations.map((inv) => (
                  <li
                    key={inv.id}
                    className="list-group-item px-0 d-flex flex-wrap justify-content-between align-items-center gap-2"
                  >
                    <span>
                      <strong>{inv.invitedByName}</strong> invited you to a {TRACK_LABELS[inv.track]}{' '}
                      group for {inv.semester}.
                    </span>
                    <span className="d-flex gap-2">
                      <button
                        type="button"
                        className="btn btn-primary btn-sm"
                        disabled={busy}
                        onClick={() => handleRespond(inv, true)}
                      >
                        Accept
                      </button>
                      <button
                        type="button"
                        className="btn btn-outline-secondary btn-sm"
                        disabled={busy}
                        onClick={() => handleRespond(inv, false)}
                      >
                        Decline
                      </button>
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </>
      ) : (
        <>
          <div className="tm-card p-4 mb-4">
            <div className="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-3">
              <div>
                <h2 className="h5 mb-1">{TRACK_LABELS[group.track]} group</h2>
                <div className="small text-secondary">
                  {group.semester} &middot; {group.members.length} of {maxSize}{' '}
                  {maxSize === 1 ? 'member' : 'members'}
                </div>
              </div>
              <span className="badge text-bg-light border text-dark">{group.status}</span>
            </div>

            <ul className="list-group list-group-flush">
              {group.members.map((m) => (
                <li
                  key={m.userId}
                  className="list-group-item px-0 d-flex flex-wrap justify-content-between align-items-center gap-2"
                >
                  <span>
                    {m.fullName}{' '}
                    <span className="text-secondary small">({m.studentId})</span>
                    {m.leader && <span className="badge text-bg-primary ms-2">Leader</span>}
                  </span>
                  {isLeader && !m.leader && (
                    <button
                      type="button"
                      className="btn btn-outline-secondary btn-sm"
                      disabled={busy}
                      onClick={() => handleTransfer(m)}
                    >
                      Make leader
                    </button>
                  )}
                </li>
              ))}
            </ul>
          </div>

          {isLeader && (
            <div className="tm-card p-4">
              <h2 className="h5 mb-3">Invite a member</h2>
              {group.track === 'INTERNSHIP' ? (
                <p className="text-secondary mb-0">Internship groups have exactly one member.</p>
              ) : canInvite ? (
                <form onSubmit={handleInvite} className="row g-3 align-items-end">
                  <div className="col-sm-8">
                    <label htmlFor="invite-student-id" className="form-label">Student ID</label>
                    <input
                      id="invite-student-id"
                      className="form-control"
                      value={inviteId}
                      onChange={(e) => setInviteId(e.target.value)}
                      inputMode="numeric"
                      pattern="[0-9]{8}"
                      title="8 digits"
                      placeholder="8-digit student ID"
                      required
                    />
                  </div>
                  <div className="col-sm-4">
                    <button type="submit" className="btn btn-primary w-100" disabled={busy}>
                      Send invitation
                    </button>
                  </div>
                </form>
              ) : (
                <p className="text-secondary mb-0">The group is full.</p>
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
}