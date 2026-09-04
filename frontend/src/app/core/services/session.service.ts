import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { ApiResponse, Page } from '../models/api-response.model';
import { TrainingSession } from '../models/session.model';

@Injectable({ providedIn: 'root' })
export class SessionService {
  constructor(private readonly http: HttpClient) {}

  list(params: Record<string, string | number | boolean | undefined> = {}): Observable<Page<TrainingSession>> {
    let httpParams = new HttpParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== '') httpParams = httpParams.set(key, String(value));
    });
    return this.http.get<ApiResponse<Page<TrainingSession>>>(`${API_BASE_URL}/sessions`, { params: httpParams }).pipe(map((r) => r.data));
  }

  byFormation(formationId: number): Observable<TrainingSession[]> {
    return this.http.get<ApiResponse<TrainingSession[]>>(`${API_BASE_URL}/formations/${formationId}/sessions`).pipe(map((r) => r.data));
  }

  get(id: number): Observable<TrainingSession> {
    return this.http.get<ApiResponse<TrainingSession>>(`${API_BASE_URL}/sessions/${id}`).pipe(map((r) => r.data));
  }

  mineAsTrainer(): Observable<TrainingSession[]> {
    return this.http.get<ApiResponse<TrainingSession[]>>(`${API_BASE_URL}/trainers/me/sessions`).pipe(map((r) => r.data));
  }

  create(payload: unknown): Observable<TrainingSession> {
    return this.http.post<ApiResponse<TrainingSession>>(`${API_BASE_URL}/sessions`, payload).pipe(map((r) => r.data));
  }

  update(id: number, payload: unknown): Observable<TrainingSession> {
    return this.http.put<ApiResponse<TrainingSession>>(`${API_BASE_URL}/sessions/${id}`, payload).pipe(map((r) => r.data));
  }

  updateStatus(id: number, status: string): Observable<TrainingSession> {
    return this.http.patch<ApiResponse<TrainingSession>>(`${API_BASE_URL}/sessions/${id}/status`, { status }).pipe(map((r) => r.data));
  }

  start(id: number): Observable<TrainingSession> {
    return this.http.post<ApiResponse<TrainingSession>>(`${API_BASE_URL}/sessions/${id}/start`, {}).pipe(map((r) => r.data));
  }

  remind(id: number): Observable<TrainingSession> {
    return this.http.post<ApiResponse<TrainingSession>>(`${API_BASE_URL}/sessions/${id}/remind`, {}).pipe(map((r) => r.data));
  }

  cancel(id: number): Observable<TrainingSession> {
    return this.http.post<ApiResponse<TrainingSession>>(`${API_BASE_URL}/sessions/${id}/cancel`, {}).pipe(map((r) => r.data));
  }

  complete(id: number, presentEnrollmentIds: number[]): Observable<TrainingSession> {
    return this.http.post<ApiResponse<TrainingSession>>(`${API_BASE_URL}/sessions/${id}/complete`, { presentEnrollmentIds }).pipe(map((r) => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/sessions/${id}`);
  }
}
