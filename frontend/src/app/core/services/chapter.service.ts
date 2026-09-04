import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { ApiResponse } from '../models/api-response.model';
import { Chapter } from '../models/catalogue.model';

@Injectable({ providedIn: 'root' })
export class ChapterService {
  constructor(private readonly http: HttpClient) {}

  byFormation(formationId: number): Observable<Chapter[]> {
    return this.http.get<ApiResponse<Chapter[]>>(`${API_BASE_URL}/formations/${formationId}/chapters`).pipe(map((r) => r.data));
  }

  create(formationId: number, payload: Partial<Chapter>): Observable<Chapter> {
    return this.http.post<ApiResponse<Chapter>>(`${API_BASE_URL}/formations/${formationId}/chapters`, payload).pipe(map((r) => r.data));
  }

  update(id: number, payload: Partial<Chapter>): Observable<Chapter> {
    return this.http.put<ApiResponse<Chapter>>(`${API_BASE_URL}/chapters/${id}`, payload).pipe(map((r) => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/chapters/${id}`);
  }
}
