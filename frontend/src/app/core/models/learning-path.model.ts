export type LearningPathStepStatus = 'COMPLETED' | 'IN_PROGRESS' | 'RECOMMENDED_NEXT' | 'AVAILABLE' | 'LOCKED';

export interface LearningPathStep {
  order: number;
  formationId: number;
  formationTitle: string;
  categoryName: string;
  level: string;
  durationHours: number;
  status: LearningPathStepStatus;
  matchPercentage: number;
  requiredSkills: string[];
  matchingSkills: string[];
  missingSkills: string[];
  hasAvailableSession: boolean;
  reason: string;
  formationProgressPercentage: number;
  completedSessions: number;
  totalSessions: number;
}

export interface LearningPath {
  learnerId: number;
  learnerFullName: string;
  goal?: string;
  currentLevel?: string;
  globalProgressPercentage: number;
  estimatedTotalHours: number;
  completedSteps: number;
  totalSteps: number;
  nextRecommendedFormationId?: number;
  nextRecommendedFormationTitle?: string;
  steps: LearningPathStep[];
}
