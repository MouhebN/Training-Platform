import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { ApiResponse } from '../models/api-response.model';
import { LearningPath } from '../models/learning-path.model';

@Injectable({ providedIn: 'root' })
export class LearningPathService {
  constructor(private readonly http: HttpClient) {}

  mine(): Observable<LearningPath> {
    return this.http.get<ApiResponse<LearningPath>>(`${API_BASE_URL}/learning-path/me`).pipe(map((r) => r.data));
  }
}
