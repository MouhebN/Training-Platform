import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable, tap } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { API_BASE_URL } from './api.config';
import { ApiResponse } from '../models/api-response.model';
import { ImprovementPlan, LearnerProfile, LearnerProfileScore, SkillGapAnalysis } from '../models/profile.model';

interface LearnerProfileUpdate {
  profile: LearnerProfile;
  token: string;
}

@Injectable({ providedIn: 'root' })
export class LearnerService {
  constructor(private readonly http: HttpClient, private readonly auth: AuthService) {}

  list(): Observable<LearnerProfile[]> {
    return this.http.get<ApiResponse<LearnerProfile[]>>(`${API_BASE_URL}/learners`).pipe(map((r) => r.data));
  }

  me(): Observable<LearnerProfile> {
    return this.http.get<ApiResponse<LearnerProfile>>(`${API_BASE_URL}/learners/me`).pipe(map((r) => r.data));
  }

  updateMe(payload: unknown): Observable<LearnerProfile> {
    return this.http.put<ApiResponse<LearnerProfileUpdate>>(`${API_BASE_URL}/learners/me`, payload).pipe(
      tap((response) => this.auth.replaceSession(response.data.token, response.data.profile.user)),
      map((r) => r.data.profile)
    );
  }

  profileScore(): Observable<LearnerProfileScore> {
    return this.http.get<ApiResponse<LearnerProfileScore>>(`${API_BASE_URL}/learners/me/profile-score`).pipe(map((r) => r.data));
  }

  skillGap(formationId: number): Observable<SkillGapAnalysis> {
    return this.http.get<ApiResponse<SkillGapAnalysis>>(`${API_BASE_URL}/learners/me/skill-gap/${formationId}`).pipe(map((r) => r.data));
  }

  improvementPlan(): Observable<ImprovementPlan> {
    return this.http.get<ApiResponse<ImprovementPlan>>(`${API_BASE_URL}/learners/me/improvement-plan`).pipe(map((r) => r.data));
  }
}
