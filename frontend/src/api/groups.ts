import { api } from './client';

export type Track = 'THESIS' | 'PROJECT' | 'INTERNSHIP';

export const TRACK_LABELS: Record<Track, string> = {
  THESIS: 'Thesis',
  PROJECT: 'Project',
  INTERNSHIP: 'Internship',
};

export interface GroupMemberInfo {
  userId: number;
  fullName: string;
  studentId: string | null;
  leader: boolean;
}

export interface Group {
  id: number;
  track: Track;
  semester: string;
  status: string;
  leaderId: number;
  leaderName: string;
  members: GroupMemberInfo[];
}

export interface GroupInvitation {
  id: number;
  groupId: number;
  track: Track;
  semester: string;
  invitedByName: string;
  status: string;
  createdAt: string;
}

export const groupsApi = {
  myGroup: () => api<Group>('/groups/me'),
  myInvitations: () => api<GroupInvitation[]>('/groups/invitations/me'),
  create: (track: Track, semester: string) =>
    api<Group>('/groups', { method: 'POST', body: JSON.stringify({ track, semester }) }),
  invite: (groupId: number, studentId: string) =>
    api<GroupInvitation>(`/groups/${groupId}/invitations`, {
      method: 'POST',
      body: JSON.stringify({ studentId }),
    }),
  respond: (invitationId: number, accept: boolean) =>
    api<GroupInvitation>(`/groups/invitations/${invitationId}/${accept ? 'accept' : 'decline'}`, {
      method: 'POST',
    }),
  transferLeader: (groupId: number, studentId: string) =>
    api<Group>(`/groups/${groupId}/leader`, {
      method: 'POST',
      body: JSON.stringify({ studentId }),
    }),
};