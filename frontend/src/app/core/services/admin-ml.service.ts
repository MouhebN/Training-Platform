import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { ApiResponse } from '../models/api-response.model';

@Injectable({ providedIn: 'root' })
export class AdminMlService {
  constructor(private readonly http: HttpClient) {}

  health(): Observable<Record<string, unknown>> {
    return this.http.get<ApiResponse<Record<string, unknown>>>(`${API_BASE_URL}/admin/ml/health`).pipe(map((r) => r.data));
  }

  pipeline(): Observable<Record<string, unknown>> {
    return this.http.get<ApiResponse<Record<string, unknown>>>(`${API_BASE_URL}/admin/ml/pipeline`).pipe(map((r) => r.data));
  }

  metrics(): Observable<Record<string, unknown>> {
    return this.http.get<ApiResponse<Record<string, unknown>>>(`${API_BASE_URL}/admin/ml/metrics`).pipe(map((r) => r.data));
  }

  datasetSample(limit = 10): Observable<Record<string, unknown>> {
    return this.http
      .get<ApiResponse<Record<string, unknown>>>(`${API_BASE_URL}/admin/ml/dataset-sample`, { params: { limit } })
      .pipe(map((r) => r.data));
  }

  retrain(): Observable<Record<string, unknown>> {
    return this.http.post<ApiResponse<Record<string, unknown>>>(`${API_BASE_URL}/admin/ml/retrain`, {}).pipe(map((r) => r.data));
  }
}
