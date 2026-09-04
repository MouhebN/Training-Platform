import { User } from './user.model';

export type LearnerLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';

export interface Skill {
  id: number;
  name: string;
  description?: string;
}

export interface LearnerProfile {
  id: number;
  user: User;
  phone?: string;
  bio?: string;
  currentLevel: LearnerLevel;
  skills: Skill[];
  learningGoals?: string;
}

export interface Trainer {
  id: number;
  user: User;
  phone?: string;
  bio?: string;
  cvUrl?: string;
  yearsOfExperience: number;
  expertise: Skill[];
  averageRating: number;
  active: boolean;
}

export interface TrainerAvailability {
  id: number;
  trainerId: number;
  trainerName: string;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
}

export interface LearnerProfileScore {
  score: number;
  completedFields: string[];
  missingFields: string[];
  message: string;
}

export interface SkillGapAnalysis {
  learnerId: number;
  formationId: number;
  formationTitle: string;
  learnerSkills: string[];
  requiredSkills: string[];
  matchingSkills: string[];
  missingSkills: string[];
  matchPercentage: number;
  ready: boolean;
  recommendationMessage: string;
}

export type ImprovementPriority = 'HIGH' | 'MEDIUM' | 'LOW' | 'DONE';

export interface ImprovementSuggestion {
  formationId: number;
  formationTitle: string;
  categoryName: string;
  level: string;
  matchPercentage: number;
  missingSkills: string[];
  reasons: string[];
  priority: ImprovementPriority;
  formationProgressPercentage: number;
  completedSessions: number;
  totalSessions: number;
}

export interface ImprovementPlan {
  learnerId: number;
  profileScore: number;
  suggestionSource?: 'MLA' | 'RULES' | string;
  message?: string;
  suggestions: ImprovementSuggestion[];
}
