import { WorkloadLevel } from './planning.model';

export type IntelligenceSeverity = 'INFO' | 'WARNING' | 'CRITICAL';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'FULL';
export type ActionPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export interface IntelligenceSummary {
  totalActiveFormations: number;
  totalOpenSessions: number;
  totalConfirmedEnrollments: number;
  totalWaitlistedEnrollments: number;
  overloadedTrainerCount: number;
  highRiskSessionCount: number;
  incompleteLearnerProfileCount: number;
  highDemandFormationCount: number;
}

export interface IntelligenceAlert {
  type: string;
  severity: IntelligenceSeverity;
  title: string;
  message: string;
  relatedEntityType: string;
  relatedEntityId: number | null;
  actionLabel: string;
}

export interface HighDemandFormation {
  formationId: number;
  formationTitle: string;
  categoryName: string;
  demandScore: number;
  learnersInterestedCount: number;
  availableSessionCount: number;
  confirmedEnrollmentCount: number;
  waitlistedEnrollmentCount: number;
  reason: string;
  suggestedAction: string;
}

export interface TrainerWorkloadInsight {
  trainerId: number;
  trainerFullName: string;
  trainerEmail: string;
  totalHours: number;
  sessionCount: number;
  workloadLevel: WorkloadLevel;
  reason: string;
  suggestedAction: string;
}

export interface SessionRiskInsight {
  sessionId: number;
  sessionTitle: string;
  formationTitle: string;
  capacity: number;
  confirmedEnrollments: number;
  waitlistedEnrollments: number;
  capacityUsagePercentage: number;
  riskLevel: RiskLevel;
  reason: string;
  suggestedAction: string;
}

export interface LearnerProfileRisk {
  learnerId: number;
  learnerFullName: string;
  profileScore: number;
  missingFields: string[];
  reason: string;
  suggestedAction: string;
}

export interface MissingSkillInsight {
  skillId: number;
  skillName: string;
  missingCount: number;
  relatedFormationCount: number;
  reason: string;
  suggestedAction: string;
}

export interface RecommendedAction {
  priority: ActionPriority;
  title: string;
  description: string;
  actionType: string;
  relatedEntityType: string;
  relatedEntityId: number | null;
  actionLabel: string;
}

export interface AdminIntelligenceResponse {
  generatedAt: string;
  globalHealthScore: number;
  summary: IntelligenceSummary;
  alerts: IntelligenceAlert[];
  highDemandFormations: HighDemandFormation[];
  overloadedTrainers: TrainerWorkloadInsight[];
  sessionRisks: SessionRiskInsight[];
  learnerProfileRisks: LearnerProfileRisk[];
  topMissingSkills: MissingSkillInsight[];
  recommendedActions: RecommendedAction[];
}
