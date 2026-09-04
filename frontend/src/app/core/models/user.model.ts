export type Role = 'ADMIN' | 'TRAINER' | 'LEARNER';

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface UserAdmin {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
  enabled: boolean;
  accountLocked: boolean;
  failedLoginAttempts: number;
  createdAt: string;
}
