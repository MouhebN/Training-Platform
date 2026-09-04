import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LearningPath, LearningPathStep } from '../../../core/models/learning-path.model';
import { LearningPathService } from '../../../core/services/learning-path.service';

@Component({
  selector: 'app-learning-path',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="page-header">
      <h1>Your learning path</h1>
      <p>Your steps only: done, current, then what to take next. The bar is completion, not skill match.</p>
    </section>

    @if (path(); as p) {
      <section class="panel profile-intelligence">
        <div class="inline spread">
          <div>
            <span class="eyebrow">Goal</span>
            <h2>{{ p.goal || 'Add a learning goal on your profile' }}</h2>
            <p class="muted">Level: {{ p.currentLevel || 'Not defined' }}</p>
          </div>
          <div>
            <strong>{{ p.estimatedTotalHours }}h</strong>
            <p class="muted">on this path</p>
          </div>
        </div>
        <div class="progress-track"><span [style.width.%]="p.globalProgressPercentage"></span></div>
        <p><strong>{{ p.globalProgressPercentage }}%</strong> of your path · {{ p.completedSteps }}/{{ p.totalSteps }} steps done</p>
        @if (p.nextRecommendedFormationTitle) {
          <p>Next: <strong>{{ p.nextRecommendedFormationTitle }}</strong></p>
        }
      </section>

      <section class="timeline">
        @for (step of p.steps; track step.formationId) {
          <article class="timeline-step">
            <div class="timeline-marker">{{ step.order }}</div>
            <div class="item-card" [class.done-card]="step.status === 'COMPLETED'">
              <div class="inline spread">
                <div>
                  <h3>{{ step.formationTitle }}</h3>
                  <div class="meta">{{ step.categoryName }} · {{ step.level }} · {{ step.durationHours }}h</div>
                </div>
                <span class="badge"
                  [class.badge-success]="step.status === 'COMPLETED' || step.status === 'AVAILABLE'"
                  [class.badge-warning]="step.status === 'IN_PROGRESS' || step.status === 'RECOMMENDED_NEXT'"
                  [class.badge-danger]="step.status === 'LOCKED'">
                  {{ label(step.status) }}
                </span>
              </div>
              <div class="progress-track"><span [style.width.%]="stepProgress(step)"></span></div>
              <p><strong>{{ stepProgress(step) }}%</strong> complete · {{ sessionsLabel(step) }}</p>
              <p>{{ step.reason }}</p>
              <p class="muted">Skill fit {{ step.matchPercentage }}% · {{ step.matchingSkills.length ? 'You have: ' + step.matchingSkills.join(', ') : 'No matching skills yet' }}</p>
              @if (step.missingSkills.length && step.status !== 'COMPLETED') {
                <p class="muted">Still missing: {{ step.missingSkills.join(', ') }}</p>
              }
              @if (step.hasAvailableSession) {
                <span class="badge badge-success">Session open</span>
              }
              <a class="btn btn-primary" [routerLink]="['/learner/formations', step.formationId]">View formation</a>
            </div>
          </article>
        } @empty {
          <section class="panel"><p>No path yet. Add skills and a goal on your profile.</p></section>
        }
      </section>
    } @else {
      <section class="panel"><p class="muted">Building your path...</p></section>
    }
  `
})
export class LearningPathComponent implements OnInit {
  path = signal<LearningPath | null>(null);

  constructor(private readonly learningPathService: LearningPathService) {}

  ngOnInit(): void {
    this.learningPathService.mine().subscribe((path) => this.path.set(path));
  }

  stepProgress(step: LearningPathStep): number {
    return step.formationProgressPercentage ?? (step.status === 'COMPLETED' ? 100 : 0);
  }

  sessionsLabel(step: LearningPathStep): string {
    if (!step.totalSessions) return 'No sessions scheduled yet';
    return `${step.completedSessions}/${step.totalSessions} sessions`;
  }

  label(status: string): string {
    switch (status) {
      case 'COMPLETED': return 'Done';
      case 'IN_PROGRESS': return 'In progress';
      case 'RECOMMENDED_NEXT': return 'Next';
      case 'LOCKED': return 'Locked';
      default: return 'Later';
    }
  }
}
