import { SessionStatus } from './session.model';

export type WorkloadLevel = 'LOW' | 'NORMAL' | 'HIGH' | 'OVERLOADED';
export type ConflictSeverity = 'BLOCKING' | 'WARNING';

export interface SessionPlanningSuggestionRequest {
  formationId: number;
  preferredStartDate: string;
  preferredEndDate: string;
  durationHours: number;
  online: boolean;
  preferredCapacity: number;
}

export interface SessionPlanningSuggestion {
  trainerId: number;
  trainerFullName: string;
  trainerEmail: string;
  suggestedStartDate: string;
  suggestedEndDate: string;
  score: number;
  workloadLevel: WorkloadLevel;
  expertiseMatchPercentage: number;
  availabilityMatch: boolean;
  conflictFree: boolean;
  reasons: string[];
  warnings: string[];
}

export interface SessionConflictCheckRequest {
  formationId: number;
  trainerId: number;
  startDate: string;
  endDate: string;
  online: boolean;
  location?: string;
}

export interface SessionConflictItem {
  type: string;
  severity: ConflictSeverity;
  message: string;
  relatedSessionId?: number;
  relatedSessionTitle?: string;
}

export interface SessionConflictCheckResponse {
  hasBlockingConflicts: boolean;
  hasWarnings: boolean;
  conflicts: SessionConflictItem[];
}

export interface TrainerWorkloadSessionItem {
  sessionId: number;
  sessionTitle: string;
  formationTitle: string;
  startDate: string;
  endDate: string;
  status: SessionStatus;
  durationHours: number;
}

export interface TrainerWorkload {
  trainerId: number;
  trainerFullName: string;
  trainerEmail: string;
  sessionCount: number;
  totalHours: number;
  completedSessions: number;
  upcomingSessions: number;
  workloadLevel: WorkloadLevel;
  recommendation: string;
  sessions: TrainerWorkloadSessionItem[];
}
