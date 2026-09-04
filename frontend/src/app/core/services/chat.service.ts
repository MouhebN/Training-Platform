import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { ApiResponse, Page } from '../models/api-response.model';
import { ChatMessage, UnreadCount } from '../models/chat.model';

@Injectable({ providedIn: 'root' })
export class ChatService {
  constructor(private readonly http: HttpClient) {}

  history(sessionId: number): Observable<ChatMessage[]> {
    return this.http.get<ApiResponse<Page<ChatMessage>>>(`${API_BASE_URL}/sessions/${sessionId}/messages`, { params: { page: 0, size: 80 } })
      .pipe(map((r) => [...r.data.content].reverse()));
  }

  markAsRead(sessionId: number): Observable<UnreadCount> {
    return this.http.post<ApiResponse<UnreadCount>>(`${API_BASE_URL}/sessions/${sessionId}/messages/read`, {})
      .pipe(map((r) => r.data));
  }

  unreadCount(sessionId: number): Observable<UnreadCount> {
    return this.http.get<ApiResponse<UnreadCount>>(`${API_BASE_URL}/sessions/${sessionId}/messages/unread-count`)
      .pipe(map((r) => r.data));
  }
}
