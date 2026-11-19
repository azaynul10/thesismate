import { api } from './client';
import { Track } from './groups';
import { ProgressSummary } from './milestones';

export type WeeklyReportStatus = 'SUBMITTED' | 'APPROVED' | 'RETURNED';

export interface WeeklyReport {
  id: number;
  groupId: number;
  track: Track;
  semester: string;
  memberNames: string[];
  weekStart: string;
  weekEnd: string;
  done: string;
  next: string;
  blockers: string | null;
  status: WeeklyReportStatus;
  late: boolean;
  submittedByName: string;
  submittedAt: string;
  reviewedByName: string | null;
  reviewedAt: string | null;
  reviewComment: string | null;
}

export interface WeeklyOverview {
  groupId: number;
  track: Track;
  applicable: boolean;
  canSubmit: boolean;
  notice: string | null;
  trackingStartsOn: string | null;
  currentWeek: string;
  currentWeekSubmitted: boolean;
  missingWeeks: string[];
  returnedCount: number;
  reports: WeeklyReport[];
}

export interface WeeklyReportInput {
  done: string;
  next: string;
  blockers: string;
}

export interface GroupProgress {
  groupId: number;
  track: Track;
  semester: string;
  myRole: 'SUPERVISOR' | 'CO_SUPERVISOR';
  memberNames: string[];
  progress: ProgressSummary;
  weeklyReportsApplicable: boolean;
  missingWeeks: number;
  pendingReports: number;
  lastReportWeek: string | null;
}

export const weeklyReportsApi = {
  overview: (groupId: number) => api<WeeklyOverview>(`/groups/${groupId}/weekly-reports`),
  submit: (groupId: number, weekStart: string, input: WeeklyReportInput) =>
    api<WeeklyReport>(`/groups/${groupId}/weekly-reports/${weekStart}`, {
      method: 'POST',
      body: JSON.stringify(input),
    }),
  pending: () => api<WeeklyReport[]>('/supervisor/weekly-reports'),
  approve: (id: number, comment?: string) =>
    api<WeeklyReport>(`/supervisor/weekly-reports/${id}/approve`, {
      method: 'POST',
      body: JSON.stringify({ comment: comment ?? null }),
    }),
  returnReport: (id: number, comment: string) =>
    api<WeeklyReport>(`/supervisor/weekly-reports/${id}/return`, {
      method: 'POST',
      body: JSON.stringify({ comment }),
    }),
  progress: () => api<GroupProgress[]>('/supervisor/progress'),
};
