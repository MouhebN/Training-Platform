import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { ApiResponse } from '../models/api-response.model';
import {
  SessionConflictCheckRequest,
  SessionConflictCheckResponse,
  SessionPlanningSuggestion,
  SessionPlanningSuggestionRequest,
  TrainerWorkload
} from '../models/planning.model';

@Injectable({ providedIn: 'root' })
export class PlanningService {
  constructor(private readonly http: HttpClient) {}

  suggestions(payload: SessionPlanningSuggestionRequest): Observable<SessionPlanningSuggestion[]> {
    return this.http.post<ApiResponse<SessionPlanningSuggestion[]>>(`${API_BASE_URL}/session-planning/suggestions`, payload)
      .pipe(map((r) => r.data));
  }

  conflicts(payload: SessionConflictCheckRequest): Observable<SessionConflictCheckResponse> {
    return this.http.post<ApiResponse<SessionConflictCheckResponse>>(`${API_BASE_URL}/session-planning/conflicts`, payload)
      .pipe(map((r) => r.data));
  }

  workload(from?: string, to?: string): Observable<TrainerWorkload[]> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<ApiResponse<TrainerWorkload[]>>(`${API_BASE_URL}/trainers/workload`, { params })
      .pipe(map((r) => r.data));
  }
}
