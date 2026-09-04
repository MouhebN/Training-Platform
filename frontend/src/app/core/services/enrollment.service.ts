import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { ApiResponse } from '../models/api-response.model';
import { Enrollment, EnrollmentCancelResponse } from '../models/session.model';

@Injectable({ providedIn: 'root' })
export class EnrollmentService {
  constructor(private readonly http: HttpClient) {}

  enroll(sessionId: number): Observable<Enrollment> {
    return this.http.post<ApiResponse<Enrollment>>(`${API_BASE_URL}/sessions/${sessionId}/enroll`, {}).pipe(map((r) => r.data));
  }

  mine(): Observable<Enrollment[]> {
    return this.http.get<ApiResponse<Enrollment[]>>(`${API_BASE_URL}/enrollments/me`).pipe(map((r) => r.data));
  }

  bySession(sessionId: number): Observable<Enrollment[]> {
    return this.http.get<ApiResponse<Enrollment[]>>(`${API_BASE_URL}/sessions/${sessionId}/enrollments`).pipe(map((r) => r.data));
  }

  updateStatus(id: number, status: string): Observable<Enrollment> {
    return this.http.patch<ApiResponse<Enrollment>>(`${API_BASE_URL}/enrollments/${id}/status`, { status }).pipe(map((r) => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/enrollments/${id}`);
  }

  cancel(id: number): Observable<EnrollmentCancelResponse> {
    return this.http.patch<ApiResponse<EnrollmentCancelResponse>>(`${API_BASE_URL}/enrollments/${id}/cancel`, {})
      .pipe(map((r) => r.data));
  }
}
