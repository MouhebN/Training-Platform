import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { ApiResponse } from '../models/api-response.model';
import { Skill } from '../models/profile.model';

@Injectable({ providedIn: 'root' })
export class SkillService {
  constructor(private readonly http: HttpClient) {}

  list(): Observable<Skill[]> {
    return this.http.get<ApiResponse<Skill[]>>(`${API_BASE_URL}/skills`).pipe(map((r) => r.data));
  }

  create(payload: Partial<Skill>): Observable<Skill> {
    return this.http.post<ApiResponse<Skill>>(`${API_BASE_URL}/skills`, payload).pipe(map((r) => r.data));
  }
}
