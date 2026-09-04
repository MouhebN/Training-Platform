import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { map, Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../services/api.config';
import { ApiResponse } from '../models/api-response.model';
import { AuthResponse, ForgotPasswordResponse, LoginRequest, RegisterRequest } from '../models/auth.model';
import { Role, User } from '../models/user.model';

const TOKEN_KEY = 'training_platform_token';
const USER_KEY = 'training_platform_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly currentUser = signal<User | null>(this.readUser());

  constructor(private readonly http: HttpClient, private readonly router: Router) {}

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<ApiResponse<AuthResponse>>(`${API_BASE_URL}/auth/login`, request).pipe(
      map((response) => response.data),
      tap((auth) => this.storeAuth(auth))
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<ApiResponse<AuthResponse>>(`${API_BASE_URL}/auth/register`, request).pipe(
      map((response) => response.data),
      tap((auth) => this.storeAuth(auth))
    );
  }

  forgotPassword(email: string): Observable<ForgotPasswordResponse> {
    return this.http.post<ApiResponse<ForgotPasswordResponse>>(`${API_BASE_URL}/auth/forgot-password`, { email }).pipe(
      map((response) => response.data)
    );
  }

  resetPassword(email: string, token: string, newPassword: string): Observable<void> {
    return this.http.post<ApiResponse<void>>(`${API_BASE_URL}/auth/reset-password`, { email, token, newPassword }).pipe(
      map(() => undefined)
    );
  }

  changePassword(oldPassword: string, newPassword: string): Observable<void> {
    return this.http.put<ApiResponse<void>>(`${API_BASE_URL}/users/me/change-password`, { oldPassword, newPassword }).pipe(
      map(() => undefined)
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    return !!this.token() && !!this.currentUser();
  }

  hasRole(roles: Role[]): boolean {
    const user = this.currentUser();
    return !!user && roles.includes(user.role);
  }

  redirectAfterLogin(): void {
    const role = this.currentUser()?.role;
    if (role === 'ADMIN') this.router.navigate(['/admin/dashboard']);
    else if (role === 'TRAINER') this.router.navigate(['/trainer/dashboard']);
    else this.router.navigate(['/learner/dashboard']);
  }

  storeAuth(auth: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, auth.token);
    localStorage.setItem(USER_KEY, JSON.stringify(auth.user));
    this.currentUser.set(auth.user);
  }

  replaceSession(token: string, user: User): void {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this.currentUser.set(user);
  }

  private readUser(): User | null {
    const rawUser = localStorage.getItem(USER_KEY);
    if (!rawUser) return null;
    try {
      return JSON.parse(rawUser) as User;
    } catch {
      localStorage.removeItem(USER_KEY);
      return null;
    }
  }
}
