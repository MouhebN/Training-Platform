import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { ApiResponse } from '../models/api-response.model';

export type AppNotificationType =
  | 'CHAT_MESSAGE'
  | 'ENROLLMENT_CONFIRMED'
  | 'ENROLLMENT_WAITLISTED'
  | 'ENROLLMENT_APPROVED'
  | 'SESSION_STARTED'
  | 'SESSION_REMINDER'
  | 'SESSION_CANCELLED'
  | 'SESSION_RESCHEDULED'
  | 'SESSION_COMPLETED'
  | 'FORMATION_COMPLETED';

export interface AppNotification {
  id: number;
  type: AppNotificationType;
  title: string;
  body: string;
  link?: string;
  read: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationApiService {
  constructor(private readonly http: HttpClient) {}

  list(): Observable<AppNotification[]> {
    return this.http.get<ApiResponse<AppNotification[]>>(`${API_BASE_URL}/notifications/me`).pipe(map((r) => r.data));
  }

  unreadCount(): Observable<number> {
    return this.http.get<ApiResponse<{ unreadCount: number }>>(`${API_BASE_URL}/notifications/me/unread-count`)
      .pipe(map((r) => r.data.unreadCount));
  }

  markRead(id: number): Observable<AppNotification> {
    return this.http.post<ApiResponse<AppNotification>>(`${API_BASE_URL}/notifications/${id}/read`, {}).pipe(map((r) => r.data));
  }

  markAllRead(): Observable<void> {
    return this.http.post<ApiResponse<void>>(`${API_BASE_URL}/notifications/me/read-all`, {}).pipe(map(() => undefined));
  }
}
