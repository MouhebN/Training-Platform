import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { ApiResponse } from '../models/api-response.model';
import { AdminIntelligenceResponse } from '../models/admin-intelligence.model';

@Injectable({ providedIn: 'root' })
export class AdminIntelligenceService {
  constructor(private readonly http: HttpClient) {}

  getIntelligence(): Observable<AdminIntelligenceResponse> {
    return this.http.get<ApiResponse<AdminIntelligenceResponse>>(`${API_BASE_URL}/admin/intelligence`)
      .pipe(map((r) => r.data));
  }

  dashboard(): Observable<AdminIntelligenceResponse> {
    return this.getIntelligence();
  }
}
