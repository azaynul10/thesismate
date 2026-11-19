import { api } from './client';
import { Track } from './groups';
import { Phase } from './submissions';

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

export const TASK_STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: 'To do',
  IN_PROGRESS: 'In progress',
  DONE: 'Done',
};

export interface ProgressSummary {
  completionPercent: number;
  totalTasks: number;
  doneTasks: number;
  overdueTasks: number;
  totalMilestones: number;
  completedMilestones: number;
  overdueMilestones: number;
}

export interface TaskView {
  id: number;
  milestoneId: number;
  title: string;
  description: string | null;
  assigneeId: number | null;
  assigneeName: string | null;
  assigneeActive: boolean;
  dueDate: string | null;
  status: TaskStatus;
  overdue: boolean;
  source: 'MANUAL' | 'ACCEPTANCE_TEST' | 'MEETING_ACTION';
  sourceRef: string | null;
  createdAt: string;
  completedAt: string | null;
}

export interface MilestoneView {
  id: number;
  title: string;
  description: string | null;
  phase: Phase | null;
  dueDate: string | null;
  sortOrder: number;
  fromTemplate: boolean;
  completionPercent: number;
  totalTasks: number;
  doneTasks: number;
  overdueTasks: number;
  overdue: boolean;
  state: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';
  tasks: TaskView[];
}

export interface MilestonePlan {
  groupId: number;
  track: Track;
  semester: string;
  canEdit: boolean;
  templateApplied: boolean;
  summary: ProgressSummary;
  milestones: MilestoneView[];
}

export interface MilestoneInput {
  title: string;
  description?: string;
  phase?: Phase | null;
  dueDate?: string | null;
}

export interface TaskInput {
  title: string;
  description?: string;
  assigneeId?: number | null;
  dueDate?: string | null;
}

const json = (body: unknown) => JSON.stringify(body);

export const milestonesApi = {
  plan: (groupId: number) => api<MilestonePlan>(`/groups/${groupId}/milestones`),
  applyTemplate: (groupId: number) =>
    api<MilestonePlan>(`/groups/${groupId}/milestones/template`, { method: 'POST' }),
  addMilestone: (groupId: number, input: MilestoneInput) =>
    api<MilestoneView>(`/groups/${groupId}/milestones`, { method: 'POST', body: json(input) }),
  updateMilestone: (id: number, input: MilestoneInput) =>
    api<MilestoneView>(`/milestones/${id}`, { method: 'PUT', body: json(input) }),
  deleteMilestone: (id: number) => api<void>(`/milestones/${id}`, { method: 'DELETE' }),
  addTask: (milestoneId: number, input: TaskInput) =>
    api<TaskView>(`/milestones/${milestoneId}/tasks`, { method: 'POST', body: json(input) }),
  updateTask: (id: number, input: TaskInput) =>
    api<TaskView>(`/tasks/${id}`, { method: 'PUT', body: json(input) }),
  setStatus: (id: number, status: TaskStatus) =>
    api<TaskView>(`/tasks/${id}/status`, { method: 'PATCH', body: json({ status }) }),
  deleteTask: (id: number) => api<void>(`/tasks/${id}`, { method: 'DELETE' }),
};
