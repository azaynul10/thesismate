import { FormEvent, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, ApiError } from '../api/client';
import { AuthUser } from './LoginPage';

export type Track = 'THESIS' | 'PROJECT' | 'INTERNSHIP';

export interface SupervisorCapacityView {
  profileId: number;
  fullName: string;
  bio: string | null;
  officeRoom: string | null;
  researchAreas: string[];
  tracks: Track[];
  semester: string;
  capacity: number;
  usedSlots: number;
  remainingSlots: number;
  available: boolean;
  version: number | null;
}

const ALL_TRACKS: { id: Track; label: string }[] = [
  { id: 'THESIS', label: 'Thesis' },
  { id: 'PROJECT', label: 'Project' },
  { id: 'INTERNSHIP', label: 'Internship' },
];

const PRESET_SEMESTERS = ['Fall 2026', 'Spring 2027', 'Summer 2027'];
const BRACU_ROOM_REGEX = /^[0-9]{2}[A-Z]-[0-9]{2}[A-Z]{1,3}$/;

interface Props {
  user: AuthUser | null;
  authChecked: boolean;
}

export default function MyCapacityPage({ user, authChecked }: Props) {
  const [semester, setSemester] = useState('Fall 2026');
  const [customSemester, setCustomSemester] = useState('');
  const [useCustomSemester, setUseCustomSemester] = useState(false);

  const [view, setView] = useState<SupervisorCapacityView | null>(null);
  const [notInitialized, setNotInitialized] = useState(false);
  const [loading, setLoading] = useState(false);

  const [bio, setBio] = useState('');
  const [officeRoom, setOfficeRoom] = useState('');
  const [areasInput, setAreasInput] = useState('');
  const [tracks, setTracks] = useState<Track[]>([]);
  const [capacityInput, setCapacityInput] = useState('0');
  const [version, setVersion] = useState<number | null>(null);

  const [savingProfile, setSavingProfile] = useState(false);
  const [savingCapacity, setSavingCapacity] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const parsedAreas = Array.from(
    new Set(
      areasInput
        .split(',')
        .map((a) => a.trim())
        .filter((a) => a.length > 0)
    )
  );

  function applyView(data: SupervisorCapacityView) {
    setView(data);
    setNotInitialized(false);
    setBio(data.bio ?? '');
    setOfficeRoom(data.officeRoom ?? '');
    setAreasInput(data.researchAreas.join(', '));
    setTracks(data.tracks);
    setCapacityInput(String(data.capacity));
    setVersion(data.version);
  }

  async function load(sem: string, isCancelled?: () => boolean) {
    const trimmed = sem.trim();
    if (!trimmed) return;

    setLoading(true);
    setError('');
    setFieldErrors({});
    try {
      const data = await api<SupervisorCapacityView>(
        `/supervisor/me/capacity?semester=${encodeURIComponent(trimmed)}`
      );
      if (isCancelled?.()) return;
      applyView(data);
    } catch (err) {
      if (isCancelled?.()) return;
      if (err instanceof ApiError && err.status === 404) {
        setView(null);
        setVersion(null);
        setNotInitialized(true);
      } else {
        setError(err instanceof ApiError ? err.message : 'Failed to load capacity');
      }
    } finally {
      if (!isCancelled?.()) setLoading(false);
    }
  }

  useEffect(() => {
    if (!authChecked || user?.role !== 'SUPERVISOR') return;
    let cancelled = false;
    load(semester, () => cancelled);
    return () => {
      cancelled = true;
    };
  }, [semester, authChecked, user?.role]);

  function handleSemesterSelect(val: string) {
    setMessage('');
    if (val === '__custom__') {
      setUseCustomSemester(true);
      setCustomSemester(semester);
    } else {
      setUseCustomSemester(false);
      setSemester(val);
    }
  }

  function applyCustomSemester(e: FormEvent) {
    e.preventDefault();
    const trimmed = customSemester.trim();
    if (!trimmed) {
      setError('Semester is required.');
      return;
    }
    setMessage('');
    setSemester(trimmed);
  }

  async function saveProfile(e: FormEvent) {
    e.preventDefault();
    setError('');
    setMessage('');
    setFieldErrors({});

    const normalizedRoom = officeRoom.trim().toUpperCase();
    const nextErrors: Record<string, string> = {};
    if (normalizedRoom && !BRACU_ROOM_REGEX.test(normalizedRoom)) {
      nextErrors.officeRoom = 'Office room must match BRACU format (e.g. 07A-01C)';
    }
    if (parsedAreas.length === 0) {
      nextErrors.researchAreas = 'Enter at least one research area.';
    }
    if (tracks.length === 0) {
      nextErrors.tracks = 'Select at least one track.';
    }
    if (Object.keys(nextErrors).length > 0) {
      setFieldErrors(nextErrors);
      setError('Please fix the highlighted profile fields.');
      return;
    }

    setSavingProfile(true);
    try {
      const data = await api<SupervisorCapacityView>(
        `/supervisor/me/profile?semester=${encodeURIComponent(semester)}`,
        {
          method: 'PUT',
          body: JSON.stringify({
            bio: bio.trim() || null,
            officeRoom: normalizedRoom || null,
            researchAreas: parsedAreas,
            tracks,
          }),
        }
      );
      applyView(data);
      setMessage('Profile updated.');
    } catch (err) {
      if (err instanceof ApiError) {
        setFieldErrors(err.fieldErrors);
        setError(err.message);
      } else {
        setError('Failed to save profile.');
      }
    } finally {
      setSavingProfile(false);
    }
  }

  async function saveCapacity(e: FormEvent) {
    e.preventDefault();
    setError('');
    setMessage('');
    setFieldErrors({});

    const parsed = Number(capacityInput);
    if (capacityInput.trim() === '' || !Number.isInteger(parsed) || parsed < 0) {
      setFieldErrors({ capacity: 'Capacity must be a whole number (0 or more).' });
      return;
    }

    setSavingCapacity(true);
    try {
      const data = await api<SupervisorCapacityView>('/supervisor/me/capacity', {
        method: 'PUT',
        body: JSON.stringify({ semester, capacity: parsed, version }),
      });
      applyView(data);
      setMessage(`Capacity updated for ${data.semester}.`);
    } catch (err) {
      if (err instanceof ApiError) {
        setFieldErrors(err.fieldErrors);
        if (err.status === 409) {
          setError(`${err.message} Click Reload to fetch the latest version.`);
        } else {
          setError(err.message);
        }
      } else {
        setError('Failed to save capacity.');
      }
    } finally {
      setSavingCapacity(false);
    }
  }

  function toggleTrack(t: Track) {
    setTracks((prev) => (prev.includes(t) ? prev.filter((x) => x !== t) : [...prev, t]));
  }

  if (!authChecked) {
    return <p className="text-secondary">Checking session...</p>;
  }

  if (!user) {
    return (
      <div className="tm-card p-4">
        <h1 className="h4 mb-2">Sign in required</h1>
        <p className="text-secondary mb-3">
          Sign in with a supervisor account to manage your profile and semester capacity.
        </p>
        <Link to="/login" className="btn btn-primary btn-sm">Go to Sign in</Link>
      </div>
    );
  }

  if (user.role !== 'SUPERVISOR') {
    return (
      <div className="tm-card p-4" role="alert">
        <h1 className="h4 mb-2">Supervisor access only</h1>
        <p className="text-secondary mb-0">
          Your current role is <strong>{user.role}</strong>. Only supervisors can edit their own capacity here.
        </p>
      </div>
    );
  }

  const usedPct =
    view && view.capacity > 0 ? Math.min(100, Math.round((view.usedSlots / view.capacity) * 100)) : 0;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap justify-content-between align-items-end gap-3">
        <div>
          <h1 className="h3 mb-1">My Supervision Capacity</h1>
          <p className="text-secondary mb-0">
            Manage your public research profile, supported tracks, and semester group limit.
          </p>
        </div>

        <div className="d-flex flex-wrap align-items-end gap-2">
          <div>
            <label htmlFor="semester-select" className="form-label small text-secondary mb-1">
              Semester
            </label>
            <select
              id="semester-select"
              className="form-select form-select-sm"
              value={useCustomSemester ? '__custom__' : semester}
              onChange={(e) => handleSemesterSelect(e.target.value)}
            >
              {PRESET_SEMESTERS.map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
              {!PRESET_SEMESTERS.includes(semester) && !useCustomSemester && (
                <option value={semester}>{semester}</option>
              )}
              <option value="__custom__">Other semester...</option>
            </select>
          </div>

          {useCustomSemester && (
            <form onSubmit={applyCustomSemester} className="d-flex gap-2 align-items-end">
              <div>
                <label htmlFor="custom-semester" className="form-label small text-secondary mb-1">
                  Custom label
                </label>
                <input
                  id="custom-semester"
                  className="form-control form-control-sm"
                  maxLength={30}
                  value={customSemester}
                  onChange={(e) => setCustomSemester(e.target.value)}
                  placeholder="e.g. Fall 2027"
                />
              </div>
              <button type="submit" className="btn btn-outline-primary btn-sm">Load</button>
            </form>
          )}
        </div>
      </header>

      {error && (
        <div className="alert alert-danger d-flex flex-wrap justify-content-between align-items-center gap-2 mb-0" role="alert">
          <span>{error}</span>
          <button
            type="button"
            className="btn btn-sm btn-outline-danger"
            onClick={() => load(semester)}
          >
            Reload
          </button>
        </div>
      )}

      {message && (
        <div className="alert alert-success mb-0" role="status">
          {message}
        </div>
      )}

      {notInitialized && !error && (
        <div className="alert alert-info mb-0" role="status">
          No profile or capacity saved yet for <strong>{semester}</strong>. Fill out your profile or set a capacity below to initialize it.
        </div>
      )}

      <section aria-label="Capacity summary">
        <div className="row g-3">
          <div className="col-6 col-md-3">
            <div className="tm-card p-3 h-100">
              <div className="tm-kpi-label">Semester Limit</div>
              <div className="tm-kpi-value mt-1">{loading ? '—' : (view?.capacity ?? 0)}</div>
              <div className="small text-secondary">groups max</div>
            </div>
          </div>
          <div className="col-6 col-md-3">
            <div className="tm-card p-3 h-100">
              <div className="tm-kpi-label">Active Allocations</div>
              <div className="tm-kpi-value mt-1">{loading ? '—' : (view?.usedSlots ?? 0)}</div>
              <div className="small text-secondary">from allocation records</div>
            </div>
          </div>
          <div className="col-6 col-md-3">
            <div className="tm-card p-3 h-100">
              <div className="tm-kpi-label">Remaining Slots</div>
              <div className="tm-kpi-value mt-1">{loading ? '—' : (view?.remainingSlots ?? 0)}</div>
              <div className="progress mt-2" style={{ height: '6px' }} aria-hidden="true">
                <div
                  className={`progress-bar ${view?.available ? 'bg-primary' : 'bg-secondary'}`}
                  style={{ width: `${usedPct}%` }}
                />
              </div>
            </div>
          </div>
          <div className="col-6 col-md-3">
            <div className="tm-card p-3 h-100 d-flex flex-column justify-content-between">
              <div className="tm-kpi-label">Availability</div>
              <div className="mt-1">
                {!view ? (
                  <span className="badge text-bg-secondary">Not configured</span>
                ) : view.available ? (
                  <span className="badge text-bg-success">Open for requests</span>
                ) : (
                  <span className="badge text-bg-danger">Full</span>
                )}
              </div>
              <div className="small text-secondary">
                {view?.version !== null && view?.version !== undefined ? `Version v${view.version}` : 'No limit set'}
              </div>
            </div>
          </div>
        </div>
      </section>

      <div className="row g-4">
        <div className="col-lg-7">
          <form onSubmit={saveProfile} className="tm-card p-4 h-100" noValidate>
            <h2 className="h5 mb-3">Public Profile</h2>

            <div className="mb-3">
              <label htmlFor="bio" className="form-label">Bio &amp; supervision focus</label>
              <textarea
                id="bio"
                className={`form-control ${fieldErrors.bio ? 'is-invalid' : ''}`}
                rows={3}
                maxLength={2000}
                value={bio}
                onChange={(e) => setBio(e.target.value)}
                placeholder="Briefly describe your research interests and expectations for groups."
                aria-invalid={Boolean(fieldErrors.bio)}
                aria-describedby={fieldErrors.bio ? 'bio-error' : undefined}
              />
              {fieldErrors.bio && (
                <div id="bio-error" className="invalid-feedback">{fieldErrors.bio}</div>
              )}
            </div>

            <div className="mb-3">
              <label htmlFor="officeRoom" className="form-label">Office room</label>
              <input
                id="officeRoom"
                className={`form-control ${fieldErrors.officeRoom ? 'is-invalid' : ''}`}
                maxLength={100}
                value={officeRoom}
                onChange={(e) => setOfficeRoom(e.target.value)}
                placeholder="e.g. 07A-01C"
                aria-invalid={Boolean(fieldErrors.officeRoom)}
                aria-describedby={fieldErrors.officeRoom ? 'office-help office-error' : 'office-help'}
              />
              <div id="office-help" className="form-text">
                BRACU format: Floor + Zone - Room + Type (e.g. 07A-01C = 7th floor, Zone A, Room 01, C/L/LT).
              </div>
              {fieldErrors.officeRoom && (
                <div id="office-error" className="invalid-feedback">{fieldErrors.officeRoom}</div>
              )}
            </div>

            <div className="mb-3">
              <label htmlFor="areasInput" className="form-label">Research areas</label>
              <input
                id="areasInput"
                className={`form-control ${fieldErrors.researchAreas ? 'is-invalid' : ''}`}
                placeholder="Machine Learning, NLP, Distributed Systems"
                value={areasInput}
                onChange={(e) => setAreasInput(e.target.value)}
                aria-invalid={Boolean(fieldErrors.researchAreas)}
                aria-describedby="areas-help areas-error"
              />
              <div id="areas-help" className="form-text">Separate multiple areas with commas.</div>
              {fieldErrors.researchAreas && (
                <div id="areas-error" className="invalid-feedback d-block">
                  {fieldErrors.researchAreas}
                </div>
              )}
              {parsedAreas.length > 0 && (
                <div className="d-flex flex-wrap gap-2 mt-2" aria-label="Parsed research areas">
                  {parsedAreas.map((area) => (
                    <span key={area} className="tm-chip">{area}</span>
                  ))}
                </div>
              )}
            </div>

            <fieldset className="mb-4">
              <legend className="form-label fs-6 mb-2">Supported tracks</legend>
              <div className="d-flex flex-wrap gap-3">
                {ALL_TRACKS.map((t) => (
                  <div className="form-check" key={t.id}>
                    <input
                      className="form-check-input"
                      type="checkbox"
                      id={`track-${t.id}`}
                      checked={tracks.includes(t.id)}
                      onChange={() => toggleTrack(t.id)}
                    />
                    <label className="form-check-label" htmlFor={`track-${t.id}`}>
                      {t.label}
                    </label>
                  </div>
                ))}
              </div>
              {fieldErrors.tracks && (
                <div className="text-danger small mt-1">{fieldErrors.tracks}</div>
              )}
            </fieldset>

            <button type="submit" className="btn btn-primary" disabled={savingProfile}>
              {savingProfile ? 'Saving profile...' : 'Save profile'}
            </button>
          </form>
        </div>

        <div className="col-lg-5">
          <form onSubmit={saveCapacity} className="tm-card p-4" noValidate>
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h2 className="h5 mb-0">Capacity for {semester}</h2>
              {version !== null && (
                <span className="small text-secondary">v{version}</span>
              )}
            </div>

            <div className="mb-3">
              <label htmlFor="capacity" className="form-label">Group limit</label>
              <input
                id="capacity"
                type="number"
                min={0}
                step={1}
                className={`form-control ${fieldErrors.capacity ? 'is-invalid' : ''}`}
                value={capacityInput}
                onChange={(e) => setCapacityInput(e.target.value)}
                aria-invalid={Boolean(fieldErrors.capacity)}
                aria-describedby="capacity-help capacity-error"
              />
              <div id="capacity-help" className="form-text">
                Counted in groups. Availability closes automatically once active allocations reach this number.
              </div>
              {fieldErrors.capacity && (
                <div id="capacity-error" className="invalid-feedback d-block">
                  {fieldErrors.capacity}
                </div>
              )}
            </div>

            <button type="submit" className="btn btn-primary w-100" disabled={savingCapacity}>
              {savingCapacity ? 'Saving capacity...' : 'Save capacity'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
