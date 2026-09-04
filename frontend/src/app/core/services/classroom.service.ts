import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { ApiResponse } from '../models/api-response.model';
import { ClassroomAttendanceReport, ClassroomContext } from '../models/classroom.model';
import { TrainingSession } from '../models/session.model';

@Injectable({ providedIn: 'root' })
export class ClassroomService {
  constructor(private readonly http: HttpClient) {}

  context(sessionId: number): Observable<ClassroomContext> {
    return this.http
      .post<ApiResponse<ClassroomContext>>(`${API_BASE_URL}/sessions/${sessionId}/classroom/context`, {})
      .pipe(map((response) => response.data));
  }

  join(sessionId: number): Observable<ClassroomContext> {
    return this.http
      .post<ApiResponse<ClassroomContext>>(`${API_BASE_URL}/sessions/${sessionId}/classroom/join`, {})
      .pipe(map((response) => response.data));
  }

  heartbeat(sessionId: number): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${API_BASE_URL}/sessions/${sessionId}/classroom/heartbeat`, {})
      .pipe(map(() => undefined));
  }

  leave(sessionId: number): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${API_BASE_URL}/sessions/${sessionId}/classroom/leave`, {})
      .pipe(map(() => undefined));
  }

  attendance(sessionId: number): Observable<ClassroomAttendanceReport> {
    return this.http
      .get<ApiResponse<ClassroomAttendanceReport>>(`${API_BASE_URL}/sessions/${sessionId}/classroom/attendance`)
      .pipe(map((response) => response.data));
  }

  completeSmart(sessionId: number): Observable<TrainingSession> {
    return this.http
      .post<ApiResponse<TrainingSession>>(`${API_BASE_URL}/sessions/${sessionId}/complete-smart`, {})
      .pipe(map((response) => response.data));
  }
}
