import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { ApiResponse } from '../models/api-response.model';
import { Trainer, TrainerAvailability } from '../models/profile.model';

@Injectable({ providedIn: 'root' })
export class TrainerService {
  constructor(private readonly http: HttpClient) {}

  list(): Observable<Trainer[]> {
    return this.http.get<ApiResponse<Trainer[]>>(`${API_BASE_URL}/trainers`).pipe(map((r) => r.data));
  }

  me(): Observable<Trainer> {
    return this.http.get<ApiResponse<Trainer>>(`${API_BASE_URL}/trainers/me`).pipe(map((r) => r.data));
  }

  create(payload: unknown): Observable<Trainer> {
    return this.http.post<ApiResponse<Trainer>>(`${API_BASE_URL}/trainers`, payload).pipe(map((r) => r.data));
  }

  update(id: number, payload: unknown): Observable<Trainer> {
    return this.http.put<ApiResponse<Trainer>>(`${API_BASE_URL}/trainers/${id}`, payload).pipe(map((r) => r.data));
  }

  updateMe(payload: unknown): Observable<Trainer> {
    return this.http.put<ApiResponse<Trainer>>(`${API_BASE_URL}/trainers/me`, payload).pipe(map((r) => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/trainers/${id}`);
  }

  uploadCv(file: File): Observable<Trainer> {
    const body = new FormData();
    body.append('file', file);
    return this.http.post<ApiResponse<Trainer>>(`${API_BASE_URL}/trainers/me/cv`, body).pipe(map((r) => r.data));
  }

  downloadCv(trainerId: number): Observable<Blob> {
    return this.http.get(`${API_BASE_URL}/trainers/${trainerId}/cv`, { responseType: 'blob' });
  }

  downloadMyCv(): Observable<Blob> {
    return this.http.get(`${API_BASE_URL}/trainers/me/cv`, { responseType: 'blob' });
  }

  availability(trainerId: number): Observable<TrainerAvailability[]> {
    return this.http.get<ApiResponse<TrainerAvailability[]>>(`${API_BASE_URL}/trainers/${trainerId}/availability`).pipe(map((r) => r.data));
  }

  createAvailability(payload: unknown): Observable<TrainerAvailability> {
    return this.http.post<ApiResponse<TrainerAvailability>>(`${API_BASE_URL}/trainers/me/availability`, payload).pipe(map((r) => r.data));
  }

  updateAvailability(id: number, payload: unknown): Observable<TrainerAvailability> {
    return this.http.put<ApiResponse<TrainerAvailability>>(`${API_BASE_URL}/trainer-availability/${id}`, payload).pipe(map((r) => r.data));
  }

  deleteAvailability(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/trainer-availability/${id}`);
  }
}
