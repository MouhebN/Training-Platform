import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { ApiResponse, Page } from '../models/api-response.model';
import { Formation, FormationRequest } from '../models/catalogue.model';

@Injectable({ providedIn: 'root' })
export class FormationService {
  constructor(private readonly http: HttpClient) {}

  list(params: Record<string, string | number | boolean | undefined> = {}): Observable<Page<Formation>> {
    let httpParams = new HttpParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== '') httpParams = httpParams.set(key, String(value));
    });
    return this.http.get<ApiResponse<Page<Formation>>>(`${API_BASE_URL}/formations`, { params: httpParams }).pipe(map((r) => r.data));
  }

  get(id: number): Observable<Formation> {
    return this.http.get<ApiResponse<Formation>>(`${API_BASE_URL}/formations/${id}`).pipe(map((r) => r.data));
  }

  create(payload: FormationRequest): Observable<Formation> {
    return this.http.post<ApiResponse<Formation>>(`${API_BASE_URL}/formations`, payload).pipe(map((r) => r.data));
  }

  update(id: number, payload: FormationRequest): Observable<Formation> {
    return this.http.put<ApiResponse<Formation>>(`${API_BASE_URL}/formations/${id}`, payload).pipe(map((r) => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/formations/${id}`);
  }
}
