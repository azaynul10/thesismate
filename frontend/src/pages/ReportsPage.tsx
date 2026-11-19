import { FormEvent, useCallback, useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import { Group, groupsApi } from '../api/groups';
import { MAX_SIMILARITY, Report, reportsApi } from '../api/reports';
import { PHASES, Phase } from '../api/submissions';
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

function when(value: string): string {
  return new Date(value).toLocaleString();
}

function size(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export default function ReportsPage({ user, authChecked }: Props) {
  const isStudent = user?.role === 'STUDENT';

  const [group, setGroup] = useState<Group | null>(null);
  const [reports, setReports] = useState<Report[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  // upload form (formKey changes after a successful upload so the file inputs are cleared)
  const [formKey, setFormKey] = useState(0);
  const [phase, setPhase] = useState<Phase>('PRE_THESIS_1');
  const [similarity, setSimilarity] = useState('');
  const [reportFile, setReportFile] = useState<File | null>(null);
  const [similarityFile, setSimilarityFile] = useState<File | null>(null);
  const [aiFile, setAiFile] = useState<File | null>(null);

  const load = useCallback(async () => {
    try {
      const g = await groupsApi.myGroup();
      setGroup(g);
      setReports(await reportsApi.list(g.id));
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
    if (isStudent) {
      void load();
    }
  }, [isStudent, load]);

  const similarityNumber = similarity.trim() === '' ? null : Number(similarity);
  const similarityTooHigh = similarityNumber !== null && similarityNumber > MAX_SIMILARITY;

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!group) return;
    setError(null);
    setNotice(null);
    if (similarityNumber === null || Number.isNaN(similarityNumber)) {
      setError('Enter the Turnitin similarity percentage.');
      return;
    }
    if (!reportFile || !similarityFile || !aiFile) {
      setError('Choose all three PDF files: the report, the Turnitin similarity report and the Turnitin AI report.');
      return;
    }
    const form = new FormData();
    form.append('phase', phase);
    form.append('similarityPercent', String(similarityNumber));
    form.append('report', reportFile);
    form.append('similarityReport', similarityFile);
    form.append('aiReport', aiFile);
    setBusy(true);
    try {
      await reportsApi.submit(group.id, form);
      setReports(await reportsApi.list(group.id));
      setNotice('Report submitted as a new version.');
      setReportFile(null);
      setSimilarityFile(null);
      setAiFile(null);
      setSimilarity('');
      setFormKey((k) => k + 1);
    } catch (err) {
      setError(messageOf(err));
    } finally {
      setBusy(false);
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
        <h1 className="h4 mb-2">Reports</h1>
        <p className="text-secondary mb-0">
          This page is for students who are in a group. Your role is {user.role}.
        </p>
      </div>
    );
  }
  if (loading) {
    return <p className="text-secondary">Loading...</p>;
  }

  return (
    <div>
      <h1 className="h3 mb-3">Reports</h1>

      <div aria-live="polite">
        {error && (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        )}
        {notice && <div className="alert alert-success">{notice}</div>}
      </div>

      {!group ? (
        <div className="tm-card p-4">
          <p className="mb-0 text-secondary">
            You are not in a group yet. Create or join a group first (My Group), then you can submit reports.
          </p>
        </div>
      ) : (
        <>
          <p className="text-secondary">
            Group #{group.id} · {label(group.track)} · {group.semester}
          </p>

          <section className="tm-card p-4 mb-4" aria-labelledby="upload-title">
            <h2 id="upload-title" className="h5">Submit a report</h2>
            <p className="small text-secondary">
              Every upload becomes a new version and older versions are kept. You need the Turnitin similarity report
              (at most {MAX_SIMILARITY}%) and the Turnitin AI report. The submission window must be open.
            </p>
            <form key={formKey} onSubmit={submit}>
              <div className="row g-3">
                <div className="col-md-6">
                  <label htmlFor="phase" className="form-label">Phase</label>
                  <select
                    id="phase"
                    className="form-select"
                    value={phase}
                    onChange={(e) => setPhase(e.target.value as Phase)}
                  >
                    {PHASES.map((p) => (
                      <option key={p} value={p}>{label(p)}</option>
                    ))}
                  </select>
                </div>
                <div className="col-md-6">
                  <label htmlFor="similarity" className="form-label">Turnitin similarity (%)</label>
                  <input
                    id="similarity"
                    type="number"
                    min="0"
                    max="100"
                    step="0.01"
                    className={`form-control ${similarityTooHigh ? 'is-invalid' : ''}`}
                    value={similarity}
                    onChange={(e) => setSimilarity(e.target.value)}
                  />
                  {similarityTooHigh && (
                    <div className="invalid-feedback">The limit is {MAX_SIMILARITY}%.</div>
                  )}
                </div>
                <div className="col-12">
                  <label htmlFor="report-file" className="form-label">Report (PDF)</label>
                  <input
                    id="report-file"
                    type="file"
                    accept="application/pdf,.pdf"
                    className="form-control"
                    onChange={(e) => setReportFile(e.target.files?.[0] ?? null)}
                  />
                </div>
                <div className="col-md-6">
                  <label htmlFor="similarity-file" className="form-label">Turnitin similarity report (PDF)</label>
                  <input
                    id="similarity-file"
                    type="file"
                    accept="application/pdf,.pdf"
                    className="form-control"
                    onChange={(e) => setSimilarityFile(e.target.files?.[0] ?? null)}
                  />
                </div>
                <div className="col-md-6">
                  <label htmlFor="ai-file" className="form-label">Turnitin AI report (PDF)</label>
                  <input
                    id="ai-file"
                    type="file"
                    accept="application/pdf,.pdf"
                    className="form-control"
                    onChange={(e) => setAiFile(e.target.files?.[0] ?? null)}
                  />
                </div>
              </div>
              <button type="submit" className="btn btn-primary mt-3" disabled={busy}>
                {busy ? 'Uploading...' : 'Submit report'}
              </button>
            </form>
          </section>

          <section aria-labelledby="history-title">
            <h2 id="history-title" className="h5 mb-3">Version history</h2>
            {reports.length === 0 ? (
              <p className="text-secondary">Nothing submitted yet.</p>
            ) : (
              reports.map((r) => (
                <div key={r.id} className="tm-card p-4 mb-3">
                  <div className="d-flex align-items-center gap-2 mb-2">
                    <h3 className="h6 mb-0">{label(r.phase)}</h3>
                    <span className="badge text-bg-light border text-dark">{label(r.status)}</span>
                    {r.locked && <span className="badge text-bg-success">Locked</span>}
                  </div>
                  <div className="table-responsive">
                    <table className="table table-sm align-middle mb-0">
                      <thead>
                        <tr>
                          <th scope="col">Version</th>
                          <th scope="col">Report</th>
                          <th scope="col">Uploaded</th>
                          <th scope="col">Similarity</th>
                          <th scope="col">Turnitin files</th>
                        </tr>
                      </thead>
                      <tbody>
                        {r.versions.map((v) => (
                          <tr key={v.id}>
                            <td>
                              v{v.versionNo}
                              {v.late && <span className="badge text-bg-warning ms-1">Late</span>}
                              {v.locked && <span className="badge text-bg-success ms-1">Locked</span>}
                            </td>
                            <td>
                              <a href={reportsApi.fileUrl(v.id, 'REPORT')}>{v.originalName}</a>
                              <div className="small text-secondary">{size(v.sizeBytes)}</div>
                            </td>
                            <td>
                              {when(v.uploadedAt)}
                              <div className="small text-secondary">{v.uploadedByName}</div>
                            </td>
                            <td>{v.similarityPercent === null ? '-' : `${v.similarityPercent}%`}</td>
                            <td>
                              <a href={reportsApi.fileUrl(v.id, 'SIMILARITY')}>Similarity</a>
                              {' · '}
                              <a href={reportsApi.fileUrl(v.id, 'AI')}>AI</a>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              ))
            )}
          </section>
        </>
      )}
    </div>
  );
}
