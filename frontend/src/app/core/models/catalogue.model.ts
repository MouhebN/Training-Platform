import { Skill } from './profile.model';

export type FormationLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';

export interface Category {
  id: number;
  name: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Formation {
  id: number;
  title: string;
  description?: string;
  price?: number;
  level: FormationLevel;
  durationHours: number;
  sessionCount: number;
  active: boolean;
  category: Category;
  requiredSkills: Skill[];
  createdAt?: string;
  updatedAt?: string;
}

export interface FormationRequest {
  title: string;
  description?: string;
  price?: number;
  level: FormationLevel;
  durationHours: number;
  sessionCount: number;
  active: boolean;
  categoryId: number;
  requiredSkillIds?: number[];
}

export interface Chapter {
  id: number;
  title: string;
  content?: string;
  orderIndex: number;
  formationId: number;
  formationTitle: string;
  createdAt?: string;
  updatedAt?: string;
}
