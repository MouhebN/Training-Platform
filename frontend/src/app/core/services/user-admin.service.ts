import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { ApiResponse } from '../models/api-response.model';
import { UserAdmin } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserAdminService {
  constructor(private readonly http: HttpClient) {}

  list(): Observable<UserAdmin[]> {
    return this.http.get<ApiResponse<UserAdmin[]>>(`${API_BASE_URL}/admin/users`).pipe(map((r) => r.data));
  }

  activate(id: number): Observable<UserAdmin> {
    return this.http.patch<ApiResponse<UserAdmin>>(`${API_BASE_URL}/admin/users/${id}/activate`, {}).pipe(map((r) => r.data));
  }

  deactivate(id: number): Observable<UserAdmin> {
    return this.http.patch<ApiResponse<UserAdmin>>(`${API_BASE_URL}/admin/users/${id}/deactivate`, {}).pipe(map((r) => r.data));
  }

  unlock(id: number): Observable<UserAdmin> {
    return this.http.patch<ApiResponse<UserAdmin>>(`${API_BASE_URL}/admin/users/${id}/unlock`, {}).pipe(map((r) => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/admin/users/${id}`);
  }
}
