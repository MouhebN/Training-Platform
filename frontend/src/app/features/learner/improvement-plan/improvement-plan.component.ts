import { Component, OnInit, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ImprovementPlan } from '../../../core/models/profile.model';
import { LearnerService } from '../../../core/services/learner.service';

@Component({
  selector: 'app-improvement-plan',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="page-header">
      <h1>Improvement plan</h1>
      <p>Recommended formations based on your profile, goals, and skills.</p>
    </section>

    @if (plan(); as p) {
      <section class="panel profile-intelligence">
        <div>
          <span class="eyebrow">Profile score</span>
          <h2>{{ p.profileScore }}%</h2>
        </div>
        <div class="progress-track"><span [style.width.%]="p.profileScore"></span></div>
        <div class="inline spread" style="margin-top:12px;gap:10px;flex-wrap:wrap;">
          <span class="badge"
            [class.badge-success]="p.suggestionSource === 'MLA'"
            [class.badge-warning]="p.suggestionSource === 'RULES'">
            {{ p.suggestionSource === 'MLA' ? 'MLA (Python model)' : 'Rules fallback' }}
          </span>
          @if (p.message) {
            <span class="muted">{{ p.message }}</span>
          }
        </div>
      </section>

      <h2>Recommended next</h2>
      <section class="card-grid">
        @for (suggestion of next(); track suggestion.formationId) {
          <article class="item-card">
            <div class="inline spread">
              <h3>{{ suggestion.formationTitle }}</h3>
              <span class="badge"
                [class.badge-success]="suggestion.priority === 'HIGH'"
                [class.badge-warning]="suggestion.priority === 'MEDIUM'"
                [class.badge-danger]="suggestion.priority === 'LOW'">
                {{ suggestion.priority }}
              </span>
            </div>
            <div class="meta">{{ suggestion.categoryName }} · {{ suggestion.level }}</div>
            @if (suggestion.totalSessions > 0) {
              <div class="progress-track"><span [style.width.%]="suggestion.formationProgressPercentage"></span></div>
              <strong>{{ suggestion.formationProgressPercentage }}% · {{ suggestion.completedSessions }}/{{ suggestion.totalSessions }} sessions</strong>
            } @else {
              <div class="progress-track"><span [style.width.%]="suggestion.matchPercentage"></span></div>
              <strong>{{ suggestion.matchPercentage }}% skill match</strong>
            }
            @if (suggestion.missingSkills.length) {
              <p class="muted">Missing: {{ suggestion.missingSkills.join(', ') }}</p>
            }
            <ul class="reason-list">
              @for (reason of suggestion.reasons; track reason) {
                <li>{{ reason }}</li>
              }
            </ul>
            <a class="btn btn-primary" [routerLink]="['/learner/formations', suggestion.formationId]">View formation</a>
          </article>
        } @empty {
          <section class="panel">
            <p>{{ p.message || 'No formation currently suited to your profile.' }}</p>
            <p class="muted">Add skills and clear learning goals in your profile, then refresh this page.</p>
            <a class="btn btn-light" routerLink="/learner/profile">Update profile</a>
          </section>
        }
      </section>

      @if (done().length) {
        <h2>Already completed</h2>
        <section class="card-grid">
          @for (suggestion of done(); track suggestion.formationId) {
            <article class="item-card done-card">
              <div class="inline spread">
                <h3>{{ suggestion.formationTitle }}</h3>
                <span class="badge badge-success">DONE</span>
              </div>
              <div class="meta">{{ suggestion.categoryName }} · {{ suggestion.level }}</div>
              <div class="progress-track"><span [style.width.%]="100"></span></div>
              <strong>100% · {{ suggestion.totalSessions || suggestion.completedSessions }} sessions completed</strong>
              <ul class="reason-list">
                @for (reason of suggestion.reasons; track reason) {
                  <li>{{ reason }}</li>
                }
              </ul>
            </article>
          }
        </section>
      }
    } @else {
      <section class="panel"><p class="muted">Loading your improvement plan...</p></section>
    }
  `
})
export class ImprovementPlanComponent implements OnInit {
  plan = signal<ImprovementPlan | null>(null);
  next = computed(() => this.plan()?.suggestions.filter((suggestion) => suggestion.priority !== 'DONE') ?? []);
  done = computed(() => this.plan()?.suggestions.filter((suggestion) => suggestion.priority === 'DONE') ?? []);

  constructor(private readonly learnerService: LearnerService) {}

  ngOnInit(): void {
    this.learnerService.improvementPlan().subscribe((plan) => this.plan.set(plan));
  }
}
