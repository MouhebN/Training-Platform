import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { ApiResponse } from '../models/api-response.model';
import { Category } from '../models/catalogue.model';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  constructor(private readonly http: HttpClient) {}

  list(): Observable<Category[]> {
    return this.http.get<ApiResponse<Category[]>>(`${API_BASE_URL}/categories`).pipe(map((r) => r.data));
  }

  create(payload: Partial<Category>): Observable<Category> {
    return this.http.post<ApiResponse<Category>>(`${API_BASE_URL}/categories`, payload).pipe(map((r) => r.data));
  }

  update(id: number, payload: Partial<Category>): Observable<Category> {
    return this.http.put<ApiResponse<Category>>(`${API_BASE_URL}/categories/${id}`, payload).pipe(map((r) => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/categories/${id}`);
  }
}
