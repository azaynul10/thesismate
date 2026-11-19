import { FormEvent, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, ApiError } from '../api/client';
import { AuthUser } from './LoginPage';
import { Track } from './MyCapacityPage';

export interface SupervisorDirectoryItem {
  profileId: number;
  supervisorId: number;
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
}

export interface SupervisorPageResponse {
  items: SupervisorDirectoryItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

interface CapacityOverrideView {
  id: number;
  semester: string;
  oldCapacity: number;
  newCapacity: number;
  reason: string;
  overriddenBy: string;
  createdAt: string;
}

const PRESET_SEMESTERS = ['Spring 2027', 'Fall 2026', 'Summer 2027'];

const SORT_OPTIONS = [
  { value: 'name,asc', label: 'Name (A–Z)' },
  { value: 'name,desc', label: 'Name (Z–A)' },
  { value: 'remaining,desc', label: 'Most free slots' },
  { value: 'remaining,asc', label: 'Fewest free slots' },
  { value: 'capacity,desc', label: 'Highest capacity' },
  { value: 'capacity,asc', label: 'Lowest capacity' },
];

function getInitials(fullName: string): string {
  const cleaned = fullName.replace(/^(Dr\.|Prof\.|Mr\.|Ms\.|Mrs\.)\s+/i, '').trim();
  const parts = cleaned.split(/\s+/).filter(Boolean);
  if (parts.length === 0) return 'SV';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

interface Props {
  user: AuthUser | null;
  authChecked: boolean;
}

export default function SupervisorDirectoryPage({ user, authChecked }: Props) {
  const [semester, setSemester] = useState('Spring 2027');
  const [area, setArea] = useState('');
  const [keyword, setKeyword] = useState('');
  const [track, setTrack] = useState<string>('');
  const [slotFilter, setSlotFilter] = useState<'all' | 'open' | 'full'>('all');
  const [sort, setSort] = useState('name,asc');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(6);

  const [data, setData] = useState<SupervisorPageResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Coordinator override panel state (F1 coordinator override + audit log)
  const [activeOverrideId, setActiveOverrideId] = useState<number | null>(null);
  const [overrideCapacity, setOverrideCapacity] = useState<number>(0);
  const [overrideReason, setOverrideReason] = useState('');
  const [overrideHistory, setOverrideHistory] = useState<CapacityOverrideView[]>([]);
  const [overrideLoading, setOverrideLoading] = useState(false);
  const [overrideMessage, setOverrideMessage] = useState('');
  const [overrideError, setOverrideError] = useState('');

  const isCoordinator = user?.role === 'COORDINATOR';
  const hasActiveFilters = Boolean(area.trim() || keyword.trim() || track || slotFilter !== 'all' || sort !== 'name,asc');

  async function loadSupervisors(opts?: {
    page?: number;
    area?: string;
    keyword?: string;
    track?: string;
    slotFilter?: 'all' | 'open' | 'full';
    sort?: string;
  }) {
    if (!user) return;
    const targetPage = opts?.page ?? page;
    const targetArea = opts?.area !== undefined ? opts.area : area;
    const targetKeyword = opts?.keyword !== undefined ? opts.keyword : keyword;
    const targetTrack = opts?.track !== undefined ? opts.track : track;
    const targetSlotFilter = opts?.slotFilter ?? slotFilter;
    const targetSort = opts?.sort ?? sort;

    setLoading(true);
    setError('');
    try {
      const params = new URLSearchParams();
      params.set('semester', semester);
      if (targetArea.trim()) params.set('area', targetArea.trim());
      if (targetKeyword.trim()) params.set('q', targetKeyword.trim());
      if (targetTrack) params.set('track', targetTrack);
      if (targetSlotFilter === 'open') params.set('hasSlots', 'true');
      if (targetSlotFilter === 'full') params.set('hasSlots', 'false');
      params.set('sort', targetSort);
      params.set('page', String(targetPage));
      params.set('size', String(size));

      const res = await api<SupervisorPageResponse>(`/supervisors?${params.toString()}`);
      setData(res);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('Failed to load supervisors.');
      }
    } finally {
      setLoading(false);
    }
  }

  // Live reactive filtering (with 180ms debounce on text inputs so typing feels instantaneous)
  useEffect(() => {
    if (!user) return;
    const timer = window.setTimeout(() => {
      void loadSupervisors({ page });
    }, 180);
    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, semester, area, keyword, track, slotFilter, sort, page, size]);

  function handleSearchSubmit(e: FormEvent) {
    e.preventDefault();
    if (page !== 0) {
      setPage(0);
    } else {
      void loadSupervisors({ page: 0 });
    }
  }

  function handleResetFilters() {
    setArea('');
    setKeyword('');
    setTrack('');
    setSlotFilter('all');
    setSort('name,asc');
    setPage(0);
  }

  function handleAreaChipClick(selectedArea: string) {
    const nextArea = area.trim().toLowerCase() === selectedArea.toLowerCase() ? '' : selectedArea;
    setArea(nextArea);
    setPage(0);
  }

  async function toggleCoordinatorPanel(item: SupervisorDirectoryItem) {
    if (activeOverrideId === item.profileId) {
      setActiveOverrideId(null);
      return;
    }
    setActiveOverrideId(item.profileId);
    setOverrideCapacity(item.capacity);
    setOverrideReason('');
    setOverrideMessage('');
    setOverrideError('');
    setOverrideLoading(true);
    try {
      const list = await api<CapacityOverrideView[]>(
        `/coordinator/supervisors/${item.profileId}/overrides`
      );
      setOverrideHistory(list);
    } catch (err) {
      setOverrideError(err instanceof ApiError ? err.message : 'Failed to load override history.');
    } finally {
      setOverrideLoading(false);
    }
  }

  async function submitOverride(e: FormEvent, profileId: number) {
    e.preventDefault();
    setOverrideError('');
    setOverrideMessage('');
    if (!overrideReason.trim()) {
      setOverrideError('Reason is required for a coordinator override.');
      return;
    }
    setOverrideLoading(true);
    try {
      await api(`/coordinator/supervisors/${profileId}/capacity`, {
        method: 'PUT',
        body: JSON.stringify({
          semester,
          newCapacity: overrideCapacity,
          reason: overrideReason.trim(),
        }),
      });
      setOverrideReason('');
      setOverrideMessage('Capacity overridden and logged to audit history.');
      const list = await api<CapacityOverrideView[]>(
        `/coordinator/supervisors/${profileId}/overrides`
      );
      setOverrideHistory(list);
      await loadSupervisors({ page });
    } catch (err) {
      setOverrideError(err instanceof ApiError ? err.message : 'Override failed.');
    } finally {
      setOverrideLoading(false);
    }
  }

  if (!authChecked) {
    return (
      <div className="py-5 text-center text-secondary" role="status">
        Checking session...
      </div>
    );
  }

  if (!user) {
    return (
      <div className="tm-card p-4">
        <h1 className="h5 mb-2">Sign in required</h1>
        <p className="text-secondary mb-3">
          Please sign in to browse the supervisor directory.
        </p>
        <Link to="/login" className="btn btn-primary btn-sm">
          Go to Sign in
        </Link>
      </div>
    );
  }

  return (
    <div className="d-flex flex-column gap-4">
      <div className="d-flex flex-wrap justify-content-between align-items-end gap-3">
        <div>
          <h1 className="h3 mb-1">Supervisor Directory</h1>
          <p className="text-secondary mb-0">
            Browse BRACU CSE supervisors by research area, track, office room, and live slot availability.
          </p>
        </div>

        <div className="d-flex align-items-center gap-2">
          <label htmlFor="dir-semester" className="form-label mb-0 small fw-semibold">
            Semester
          </label>
          <select
            id="dir-semester"
            className="form-select form-select-sm"
            style={{ width: '160px' }}
            value={semester}
            onChange={(e) => {
              setSemester(e.target.value);
              setPage(0);
            }}
          >
            {PRESET_SEMESTERS.map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Filter Bar */}
      <form onSubmit={handleSearchSubmit} className="tm-card p-3">
        <div className="row g-3 align-items-end">
          <div className="col-md-3">
            <label htmlFor="dir-keyword" className="form-label small fw-medium mb-1">
              Search faculty or room
            </label>
            <input
              id="dir-keyword"
              type="search"
              className="form-control form-control-sm"
              placeholder="Name, bio, or 07A-01C"
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value);
                setPage(0);
              }}
            />
          </div>

          <div className="col-md-3">
            <label htmlFor="dir-area" className="form-label small fw-medium mb-1">
              Research area
            </label>
            <input
              id="dir-area"
              type="search"
              className="form-control form-control-sm"
              placeholder="e.g. Machine Learning"
              value={area}
              onChange={(e) => {
                setArea(e.target.value);
                setPage(0);
              }}
            />
          </div>

          <div className="col-sm-4 col-md-2">
            <label htmlFor="dir-track" className="form-label small fw-medium mb-1">
              Track
            </label>
            <select
              id="dir-track"
              className="form-select form-select-sm"
              value={track}
              onChange={(e) => {
                setTrack(e.target.value);
                setPage(0);
              }}
            >
              <option value="">All tracks</option>
              <option value="THESIS">Thesis</option>
              <option value="PROJECT">Project</option>
              <option value="INTERNSHIP">Internship</option>
            </select>
          </div>

          <div className="col-sm-4 col-md-2">
            <label htmlFor="dir-slots" className="form-label small fw-medium mb-1">
              Availability
            </label>
            <select
              id="dir-slots"
              className="form-select form-select-sm"
              value={slotFilter}
              onChange={(e) => {
                setSlotFilter(e.target.value as 'all' | 'open' | 'full');
                setPage(0);
              }}
            >
              <option value="all">All supervisors</option>
              <option value="open">Has free slots</option>
              <option value="full">Full (0 slots)</option>
            </select>
          </div>

          <div className="col-sm-4 col-md-2">
            <label htmlFor="dir-sort" className="form-label small fw-medium mb-1">
              Sort by
            </label>
            <select
              id="dir-sort"
              className="form-select form-select-sm"
              value={sort}
              onChange={(e) => {
                setSort(e.target.value);
                setPage(0);
              }}
            >
              {SORT_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </div>
        </div>

        <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mt-3 pt-2 border-top">
          <div className="d-flex flex-wrap align-items-center gap-2">
            <span className="small text-secondary" role="status" aria-live="polite">
              {loading
                ? 'Updating directory...'
                : data
                ? `Showing ${data.items.length} of ${data.totalElements} supervisor${data.totalElements === 1 ? '' : 's'} for ${semester}`
                : 'Ready'}
            </span>
            {area.trim() && (
              <button
                type="button"
                className="tm-chip tm-chip-active"
                onClick={() => {
                  setArea('');
                  setPage(0);
                }}
                title="Clear area filter"
              >
                Area: {area.trim()} ×
              </button>
            )}
          </div>

          <div className="d-flex align-items-center gap-2">
            <label htmlFor="dir-size" className="small text-secondary mb-0">
              Per page
            </label>
            <select
              id="dir-size"
              className="form-select form-select-sm"
              style={{ width: '72px' }}
              value={size}
              onChange={(e) => {
                setSize(Number(e.target.value));
                setPage(0);
              }}
            >
              <option value={6}>6</option>
              <option value={10}>10</option>
              <option value={20}>20</option>
            </select>
            {hasActiveFilters && (
              <button type="button" className="btn btn-outline-secondary btn-sm" onClick={handleResetFilters}>
                Reset filters
              </button>
            )}
          </div>
        </div>
      </form>

      {error && (
        <div className="alert alert-danger mb-0" role="alert">
          {error}
        </div>
      )}

      {/* Results Grid */}
      {data && data.items.length === 0 && !loading ? (
        <div className="tm-card p-5 text-center">
          <h2 className="h5 mb-1">No matching supervisors</h2>
          <p className="text-secondary mb-3">
            Try clearing one or more filters or switching the semester.
          </p>
          <button type="button" className="btn btn-outline-primary btn-sm" onClick={handleResetFilters}>
            Clear all filters
          </button>
        </div>
      ) : (
        <div className="row g-3">
          {data?.items.map((item) => {
            const isOpen = activeOverrideId === item.profileId;
            const usagePercent =
              item.capacity > 0 ? Math.min(100, Math.round((item.usedSlots / item.capacity) * 100)) : 100;
            const barColor = item.available ? '#10b981' : '#94a3b8';

            return (
              <div className="col-md-6" key={item.profileId}>
                <article className="tm-card tm-card-interactive p-4 h-100 d-flex flex-column justify-content-between">
                  <div>
                    <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
                      <div className="d-flex align-items-center gap-3">
                        <div className="tm-avatar" aria-hidden="true">
                          {getInitials(item.fullName)}
                        </div>
                        <div>
                          <h2 className="h6 fw-semibold mb-1">{item.fullName}</h2>
                          <div className="d-flex align-items-center gap-2">
                            <span className="tm-room-badge" title="BRACU Office Room">
                              {item.officeRoom || 'Room TBD'}
                            </span>
                          </div>
                        </div>
                      </div>
                      <span
                        className={`tm-status-pill ${
                          item.available ? 'tm-status-open' : 'tm-status-full'
                        }`}
                      >
                        <span className="tm-status-dot" aria-hidden="true" />
                        {item.available ? `${item.remainingSlots} slot${item.remainingSlots === 1 ? '' : 's'} open` : 'Full'}
                      </span>
                    </div>

                    {item.bio && (
                      <p className="small text-secondary mb-3">{item.bio}</p>
                    )}

                    <div className="mb-3">
                      <span className="small text-secondary d-block mb-1">Supervision tracks</span>
                      <div className="d-flex flex-wrap gap-1">
                        {item.tracks.length > 0 ? (
                          item.tracks.map((t) => (
                            <span key={t} className="badge text-bg-light border text-dark fw-medium">
                              {t}
                            </span>
                          ))
                        ) : (
                          <span className="small text-secondary">None set</span>
                        )}
                      </div>
                    </div>

                    <div className="mb-3">
                      <span className="small text-secondary d-block mb-1">
                        Research areas <span className="text-muted">(click to filter)</span>
                      </span>
                      <div className="d-flex flex-wrap gap-1">
                        {item.researchAreas.length > 0 ? (
                          item.researchAreas.map((a) => {
                            const isSelected = area.trim().toLowerCase() === a.toLowerCase();
                            return (
                              <button
                                key={a}
                                type="button"
                                className={`tm-chip ${isSelected ? 'tm-chip-active' : ''}`}
                                title={isSelected ? 'Clear area filter' : `Filter by ${a}`}
                                onClick={() => handleAreaChipClick(a)}
                              >
                                {a}
                              </button>
                            );
                          })
                        ) : (
                          <span className="small text-secondary">None listed</span>
                        )}
                      </div>
                    </div>
                  </div>

                  <div>
                    <div className="border-top pt-3">
                      <div className="d-flex justify-content-between align-items-center small mb-1">
                        <span className="text-secondary">
                          Used <strong className="text-dark">{item.usedSlots}</strong> of{' '}
                          <strong className="text-dark">{item.capacity}</strong> group slots
                        </span>
                        <span className="fw-medium text-dark">
                          {item.remainingSlots} remaining
                        </span>
                      </div>
                      <div
                        className="tm-slot-meter"
                        role="progressbar"
                        aria-valuenow={item.usedSlots}
                        aria-valuemin={0}
                        aria-valuemax={Math.max(1, item.capacity)}
                        aria-label={`Slot usage for ${item.fullName}`}
                      >
                        <div
                          className="tm-slot-meter-bar"
                          style={{ width: `${usagePercent}%`, backgroundColor: barColor }}
                        />
                      </div>

                      {isCoordinator && (
                        <div className="d-flex justify-content-end mt-2">
                          <button
                            type="button"
                            className="btn btn-outline-primary btn-sm"
                            onClick={() => void toggleCoordinatorPanel(item)}
                          >
                            {isOpen ? 'Close override' : 'Override / Audit'}
                          </button>
                        </div>
                      )}
                    </div>

                    {isCoordinator && isOpen && (
                      <div className="mt-3 pt-3 border-top">
                        <h3 className="h6 mb-2">
                          Coordinator Capacity Override ({semester})
                        </h3>
                        {overrideMessage && (
                          <div className="alert alert-success py-2 small" role="status">
                            {overrideMessage}
                          </div>
                        )}
                        {overrideError && (
                          <div className="alert alert-danger py-2 small" role="alert">
                            {overrideError}
                          </div>
                        )}
                        <form onSubmit={(e) => void submitOverride(e, item.profileId)} className="row g-2 mb-3">
                          <div className="col-sm-4">
                            <label htmlFor={`override-cap-${item.profileId}`} className="form-label small mb-1">
                              New capacity
                            </label>
                            <input
                              id={`override-cap-${item.profileId}`}
                              type="number"
                              min={0}
                              className="form-control form-control-sm"
                              value={overrideCapacity}
                              onChange={(e) => setOverrideCapacity(Math.max(0, Number(e.target.value)))}
                            />
                          </div>
                          <div className="col-sm-8">
                            <label htmlFor={`override-reason-${item.profileId}`} className="form-label small mb-1">
                              Reason (required)
                            </label>
                            <input
                              id={`override-reason-${item.profileId}`}
                              type="text"
                              className="form-control form-control-sm"
                              placeholder="e.g. Approved extra thesis group"
                              value={overrideReason}
                              onChange={(e) => setOverrideReason(e.target.value)}
                            />
                          </div>
                          <div className="col-12 d-flex justify-content-end">
                            <button
                              type="submit"
                              className="btn btn-primary btn-sm"
                              disabled={overrideLoading}
                            >
                              {overrideLoading ? 'Saving...' : 'Apply override'}
                            </button>
                          </div>
                        </form>

                        <div className="small">
                          <div className="fw-semibold mb-1">Audit History</div>
                          {overrideHistory.length === 0 ? (
                            <div className="text-secondary">No overrides recorded yet.</div>
                          ) : (
                            <ul className="list-unstyled mb-0 d-flex flex-column gap-1">
                              {overrideHistory.map((h) => (
                                <li key={h.id} className="p-2 rounded bg-light border">
                                  <div>
                                    <strong>{h.semester}:</strong> {h.oldCapacity} →{' '}
                                    <strong>{h.newCapacity}</strong> ({h.reason})
                                  </div>
                                  <div className="text-secondary" style={{ fontSize: '0.75rem' }}>
                                    By {h.overriddenBy} · {new Date(h.createdAt).toLocaleString()}
                                  </div>
                                </li>
                              ))}
                            </ul>
                          )}
                        </div>
                      </div>
                    )}
                  </div>
                </article>
              </div>
            );
          })}
        </div>
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <nav className="d-flex justify-content-between align-items-center" aria-label="Directory pagination">
          <button
            type="button"
            className="btn btn-outline-secondary btn-sm"
            disabled={!data.hasPrevious || loading}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            Previous
          </button>
          <span className="small text-secondary">
            Page <strong>{data.page + 1}</strong> of <strong>{data.totalPages}</strong>
          </span>
          <button
            type="button"
            className="btn btn-outline-secondary btn-sm"
            disabled={!data.hasNext || loading}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </button>
        </nav>
      )}
    </div>
  );
}
