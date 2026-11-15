import { api } from './client';
import { Track } from './groups';

export type SupervisorRole = 'SUPERVISOR' | 'CO_SUPERVISOR';

export const ROLE_LABELS: Record<SupervisorRole, string> = {
  SUPERVISOR: 'Supervisor',
  CO_SUPERVISOR: 'Co-supervisor',
};

export type RequestStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN' | 'EXPIRED';

export interface SupervisionRequest {
  id: number;
  groupId: number;
  track: Track;
  semester: string;
  supervisorId: number;
  supervisorName: string;
  role: SupervisorRole;
  pitch: string;
  status: RequestStatus;
  rejectionReason: string | null;
  groupMembers: unknown[];
  createdAt: string;
  expiresAt: string;
  respondedAt: string | null;
}

export interface Allocation {
  id: number;
  groupId: number;
  track: Track;
  semester: string;
  supervisorId: number;
  supervisorName: string;
  role: SupervisorRole;
  groupMembers: unknown[];
  createdAt: string;
}

export interface SupervisorOption {
  supervisorId: number;
  fullName: string;
  researchAreas: string[];
  remainingSlots: number;
}

interface SupervisorPage {
  items: SupervisorOption[];
}

export const supervisionApi = {
  requests: (groupId: number) =>
    api<SupervisionRequest[]>(`/groups/${groupId}/supervision-requests`),
  allocations: (groupId: number) => api<Allocation[]>(`/groups/${groupId}/supervisors`),
  send: (groupId: number, supervisorId: number, role: SupervisorRole, pitch: string) =>
    api<SupervisionRequest>(`/groups/${groupId}/supervision-requests`, {
      method: 'POST',
      body: JSON.stringify({ supervisorId, role, pitch }),
    }),
  withdraw: (requestId: number) =>
    api<SupervisionRequest>(`/supervision-requests/${requestId}/withdraw`, { method: 'POST' }),
  availableSupervisors: async (semester: string, track: Track) => {
    const params = new URLSearchParams({
      semester,
      track,
      hasSlots: 'true',
      size: '50',
    });
    const page = await api<SupervisorPage>(`/supervisors?${params.toString()}`);
    return page.items;
  },
};