import { api } from './client';

// F15: submission windows, deliverable checklist and extensions (coordinator only).

export type Phase = 'PRE_THESIS_1' | 'PRE_THESIS_2' | 'FINAL';
export type Track = 'THESIS' | 'PROJECT' | 'INTERNSHIP';
export type LatePolicy = 'NO_LATE' | 'LATE_ALLOWED';
export type DeliverableType =
  | 'REPORT'
  | 'TURNITIN_SIMILARITY'
  | 'TURNITIN_AI'
  | 'POSTER'
  | 'IEEE_PAPER'
  | 'PROTOTYPE_RELEASE'
  | 'INDUSTRY_EVALUATION';

export const PHASES: Phase[] = ['PRE_THESIS_1', 'PRE_THESIS_2', 'FINAL'];
export const TRACKS: Track[] = ['THESIS', 'PROJECT', 'INTERNSHIP'];
export const DELIVERABLE_TYPES: DeliverableType[] = [
  'REPORT',
  'TURNITIN_SIMILARITY',
  'TURNITIN_AI',
  'POSTER',
  'IEEE_PAPER',
  'PROTOTYPE_RELEASE',
  'INDUSTRY_EVALUATION',
];

export interface SubmissionWindow {
  id: number;
  semester: string;
  phase: Phase;
  track: Track;
  opensAt: string;
  closesAt: string;
  latePolicy: LatePolicy;
  lateUntil: string | null;
  createdByName: string;
  createdAt: string;
}

export interface SubmissionWindowRequest {
  semester: string;
  phase: Phase;
  track: Track;
  opensAt: string;
  closesAt: string;
  latePolicy: LatePolicy;
  lateUntil: string | null;
}

export interface DeliverableRequirement {
  id: number;
  phase: Phase;
  track: Track;
  deliverableType: DeliverableType;
  mandatory: boolean;
}

export interface DeliverableRequirementRequest {
  phase: Phase;
  track: Track;
  deliverableType: DeliverableType;
  mandatory: boolean;
}

export interface Extension {
  id: number;
  groupId: number;
  windowId: number;
  newClosesAt: string;
  reason: string;
  grantedByName: string;
  grantedAt: string;
}

export interface ExtensionRequest {
  groupId: number;
  windowId: number;
  newClosesAt: string;
  reason: string;
}

export const submissionsApi = {
  listWindows: () => api<SubmissionWindow[]>('/coordinator/submission-windows'),
  createWindow: (body: SubmissionWindowRequest) =>
    api<SubmissionWindow>('/coordinator/submission-windows', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  updateWindow: (id: number, body: SubmissionWindowRequest) =>
    api<SubmissionWindow>(`/coordinator/submission-windows/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),

  listRequirements: () => api<DeliverableRequirement[]>('/coordinator/deliverable-requirements'),
  addRequirement: (body: DeliverableRequirementRequest) =>
    api<DeliverableRequirement>('/coordinator/deliverable-requirements', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  deleteRequirement: (id: number) =>
    api<void>(`/coordinator/deliverable-requirements/${id}`, { method: 'DELETE' }),

  listExtensions: (windowId: number) =>
    api<Extension[]>(`/coordinator/extensions?windowId=${windowId}`),
  grantExtension: (body: ExtensionRequest) =>
    api<Extension>('/coordinator/extensions', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
};
