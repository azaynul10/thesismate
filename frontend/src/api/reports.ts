import { api } from './client';
import type { Phase } from './submissions';

// F11: phase report upload, version history and file download (group members; coordinator can read).

export type ReportStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'REVISION_REQUIRED'
  | 'SUPERVISOR_APPROVED'
  | 'COMMITTEE_APPROVED'
  | 'REJECTED';

export type FileKind = 'REPORT' | 'SIMILARITY' | 'AI';

export interface ReportVersion {
  id: number;
  versionNo: number;
  originalName: string;
  sizeBytes: number;
  sha256: string;
  late: boolean;
  locked: boolean;
  uploadedByName: string;
  uploadedAt: string;
  similarityPercent: number | null;
  similarityFileName: string | null;
  aiFileName: string | null;
}

export interface Report {
  id: number;
  groupId: number;
  phase: Phase;
  status: ReportStatus;
  locked: boolean;
  versions: ReportVersion[];
}

/** Highest Turnitin similarity (percent) the backend accepts. */
export const MAX_SIMILARITY = 15;

export const reportsApi = {
  list: (groupId: number) => api<Report[]>(`/groups/${groupId}/reports`),
  // Sent as multipart/form-data: the browser sets the content type itself (see api() in client.ts).
  submit: (groupId: number, form: FormData) =>
    api<Report>(`/groups/${groupId}/reports`, { method: 'POST', body: form }),
  fileUrl: (versionId: number, kind: FileKind) => `/api/report-versions/${versionId}/file?kind=${kind}`,
};
