import { api } from './client';

export interface StudentRecord {
  studentId: string;
  creditsCompleted: number;
  cgpa: number;
  importedAt: string;
}

export interface ImportResult {
  created: number;
  updated: number;
  errors: unknown[];
}

export const studentRecordsApi = {
  list: () => api<StudentRecord[]>('/coordinator/student-records'),
  importCsv: (csv: string) =>
    api<ImportResult>('/coordinator/student-records/import', {
      method: 'POST',
      body: csv,
      headers: { 'Content-Type': 'text/csv' },
    }),
};