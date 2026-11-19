import { FormEvent, useCallback, useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import { ImportResult, StudentRecord, studentRecordsApi } from '../api/studentRecords';
import { AuthUser } from './LoginPage';

interface Props {
  user: AuthUser | null;
  authChecked: boolean;
}

function messageOf(e: unknown): string {
  return e instanceof ApiError ? e.message : 'Something went wrong. Please try again.';
}

function errorText(e: unknown): string {
  return typeof e === 'string' ? e : JSON.stringify(e);
}

export default function StudentRecordsPage({ user, authChecked }: Props) {
  const [records, setRecords] = useState<StudentRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<ImportResult | null>(null);
  const [file, setFile] = useState<File | null>(null);

  const isCoordinator = user?.role === 'COORDINATOR';

  const load = useCallback(async () => {
    try {
      setRecords(await studentRecordsApi.list());
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

  async function handleUpload(e: FormEvent) {
    e.preventDefault();
    if (!file) return;
    setBusy(true);
    setError(null);
    setResult(null);
    try {
      const text = await file.text();
      setResult(await studentRecordsApi.importCsv(text));
      await load();
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
  if (!isCoordinator) {
    return (
      <div className="tm-card p-4">
        <h1 className="h4 mb-2">Student records</h1>
        <p className="text-secondary mb-0">
          This page is for coordinators. Your role is {user.role}.
        </p>
      </div>
    );
  }

  return (
    <div>
      <h1 className="h3 mb-3">Student records</h1>

      <div aria-live="polite">
        {error && (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        )}
        {result && (
          <div className={`alert ${result.errors.length > 0 ? 'alert-warning' : 'alert-success'}`}>
            <div>
              Import finished: {result.created} created, {result.updated} updated,{' '}
              {result.errors.length} {result.errors.length === 1 ? 'error' : 'errors'}.
            </div>
            {result.errors.length > 0 && (
              <ul className="mb-0 mt-2">
                {result.errors.map((err, i) => (
                  <li key={i}>{errorText(err)}</li>
                ))}
              </ul>
            )}
          </div>
        )}
      </div>

      <div className="tm-card p-4 mb-4">
        <h2 className="h5 mb-2">Import from CSV</h2>
        <p className="small text-secondary">
          The first line must be <code>student_id,credits_completed,cgpa</code>. Existing students
          are updated, new ones are created. Eligibility checks for group formation read these
          records.
        </p>
        <form onSubmit={handleUpload} className="row g-3 align-items-end">
          <div className="col-sm-8">
            <label htmlFor="records-file" className="form-label">CSV file</label>
            <input
              id="records-file"
              type="file"
              accept=".csv,text/csv"
              className="form-control"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              required
            />
          </div>
          <div className="col-sm-4">
            <button type="submit" className="btn btn-primary w-100" disabled={busy || !file}>
              {busy ? 'Importing...' : 'Import'}
            </button>
          </div>
        </form>
      </div>

      <div className="tm-card p-4">
        <h2 className="h5 mb-3">Imported records ({records.length})</h2>
        {loading ? (
          <p className="text-secondary mb-0">Loading...</p>
        ) : records.length === 0 ? (
          <p className="text-secondary mb-0">No records yet.</p>
        ) : (
          <div className="table-responsive">
            <table className="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th scope="col">Student ID</th>
                  <th scope="col" className="text-end">Credits</th>
                  <th scope="col" className="text-end">CGPA</th>
                  <th scope="col">Imported</th>
                </tr>
              </thead>
              <tbody>
                {records.map((r) => (
                  <tr key={r.studentId}>
                    <td>{r.studentId}</td>
                    <td className="text-end">{r.creditsCompleted}</td>
                    <td className="text-end">{Number(r.cgpa).toFixed(2)}</td>
                    <td className="text-secondary small">
                      {new Date(r.importedAt).toLocaleString()}
                    </td>
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