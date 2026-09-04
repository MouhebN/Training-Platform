import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Chapter, Formation } from '../../../core/models/catalogue.model';
import { SkillGapAnalysis } from '../../../core/models/profile.model';
import { TrainingSession } from '../../../core/models/session.model';
import { ChapterService } from '../../../core/services/chapter.service';
import { EnrollmentService } from '../../../core/services/enrollment.service';
import { FormationService } from '../../../core/services/formation.service';
import { LearnerService } from '../../../core/services/learner.service';
import { SessionService } from '../../../core/services/session.service';
import { formatDateTime24 } from '../../../core/utils/date-time.util';

@Component({
  selector: 'app-formation-detail',
  standalone: true,
  imports: [RouterLink],
  template: `
    @if (formation(); as f) {
      <section class="page-header"><h1>{{ f.title }}</h1><p>{{ f.description }}</p></section>
      @if (message()) { <p class="alert success">{{ message() }}</p> }
      @if (error()) { <p class="alert error">{{ error() }}</p> }
      @if (gap(); as g) {
        <section class="panel">
          <h2>Skill gap analysis</h2>
          <div class="progress-track"><span [style.width.%]="g.matchPercentage"></span></div>
          <p><strong>{{ g.matchPercentage }}%</strong> match <span class="badge" [class.badge-success]="g.ready" [class.badge-warning]="!g.ready">{{ g.ready ? 'Ready' : 'Needs preparation' }}</span></p>
          <p>{{ g.recommendationMessage }}</p>
          <p class="muted">Required: {{ g.requiredSkills.length ? g.requiredSkills.join(', ') : 'None' }}</p>
          <p class="muted">Matching: {{ g.matchingSkills.length ? g.matchingSkills.join(', ') : 'None' }}</p>
          <p class="muted">Missing: {{ g.missingSkills.length ? g.missingSkills.join(', ') : 'None' }}</p>
        </section>
      }
      <div class="grid two">
        <section class="panel">
          <h2>Chapters</h2>
          @for (chapter of chapters(); track chapter.id) {
            <div class="list-row"><strong>{{ chapter.orderIndex }}. {{ chapter.title }}</strong><span>{{ chapter.content }}</span></div>
          }
        </section>
        <section class="panel">
          <h2>Sessions</h2>
          @for (session of sessions(); track session.id) {
            <div class="list-row">
              <strong>{{ session.title }}</strong>
              <span>{{ formatWhen(session.startDate) }} → {{ formatWhen(session.endDate) }}</span>
              <span>{{ session.status }} · {{ session.availablePlaces }} places · {{ session.online ? 'Online' : session.location }}</span>
              <button class="btn btn-primary" [disabled]="!canEnroll(session)" (click)="enroll(session)">
                {{ session.availablePlaces <= 0 ? 'Join waitlist' : 'Enroll' }}
              </button>
              <a class="btn btn-light" [routerLink]="['/learner/sessions', session.id, 'chat']">Chat</a>
            </div>
          }
        </section>
      </div>
    }
  `
})
export class FormationDetailComponent implements OnInit {
  formation = signal<Formation | null>(null);
  chapters = signal<Chapter[]>([]);
  sessions = signal<TrainingSession[]>([]);
  gap = signal<SkillGapAnalysis | null>(null);
  message = signal('');
  error = signal('');
  constructor(private readonly route: ActivatedRoute, private readonly formations: FormationService, private readonly chaptersService: ChapterService, private readonly sessionsService: SessionService, private readonly enrollments: EnrollmentService, private readonly learnerService: LearnerService) {}
  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.formations.get(id).subscribe((f) => this.formation.set(f));
    this.chaptersService.byFormation(id).subscribe((d) => this.chapters.set(d));
    this.sessionsService.byFormation(id).subscribe((d) => this.sessions.set(d));
    this.learnerService.skillGap(id).subscribe((gap) => this.gap.set(gap));
  }
  enroll(session: TrainingSession): void {
    this.enrollments.enroll(session.id).subscribe({
      next: (enrollment) => {
        this.message.set(enrollment.status === 'WAITLISTED' ? 'Session is full. You were added to the waitlist.' : 'Enrollment created.');
        this.error.set('');
        this.sessionsService.byFormation(session.formationId).subscribe((d) => this.sessions.set(d));
      },
      error: () => this.error.set('Could not enroll in this session.')
    });
  }
  canEnroll(session: TrainingSession): boolean {
    return session.status === 'OPEN' || session.status === 'PLANNED';
  }
  formatWhen(value?: string): string {
    return formatDateTime24(value);
  }
}
