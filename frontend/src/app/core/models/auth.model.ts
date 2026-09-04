import { User } from './user.model';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  user: User;
}

export interface ForgotPasswordResponse {
  message: string;
  resetToken: string | null;
  expiresAt: string | null;
}
