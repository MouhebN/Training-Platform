export type SessionStatus = 'PLANNED' | 'OPEN' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type EnrollmentStatus = 'PENDING' | 'CONFIRMED' | 'WAITLISTED' | 'CANCELLED' | 'COMPLETED';

export interface TrainingSession {
  id: number;
  formationId: number;
  formationTitle: string;
  formationSessionCount?: number;
  trainerId: number;
  trainerFullName: string;
  title: string;
  description?: string;
  startDate: string;
  endDate: string;
  capacity: number;
  enrolledCount: number;
  availablePlaces: number;
  location?: string;
  online: boolean;
  meetingUrl?: string;
  status: SessionStatus;
}

export interface Enrollment {
  id: number;
  learnerId: number;
  learnerFullName: string;
  sessionId: number;
  sessionTitle: string;
  formationId: number;
  formationTitle: string;
  formationSessionCount?: number;
  trainerFullName: string;
  status: EnrollmentStatus;
  enrolledAt: string;
  sessionStartDate?: string;
  sessionEndDate?: string;
  online?: boolean;
  location?: string;
  meetingUrl?: string;
  sessionStatus?: SessionStatus;
  virtualAttendancePercentage?: number;
  virtualAttendanceQualified?: boolean;
}

export interface EnrollmentCancelResponse {
  cancelledEnrollmentId: number;
  promoted: boolean;
  promotedEnrollmentId?: number;
  promotedLearnerFullName?: string;
  message: string;
}
