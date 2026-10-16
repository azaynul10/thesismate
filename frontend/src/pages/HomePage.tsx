import { useEffect, useState } from 'react';
import { api } from '../api/client';

export default function HomePage() {
  const [status, setStatus] = useState('checking...');

  useEffect(() => {
    api<{ status: string }>('/health')
      .then((r) => setStatus(r.status))
      .catch(() => setStatus('backend not reachable'));
  }, []);

  return (
    <div>
      <h1>ThesisMate</h1>
      <p className="text-muted">Thesis Supervision &amp; Research Management Platform</p>
      <p>Backend status: <strong>{status}</strong></p>
    </div>
  );
}
