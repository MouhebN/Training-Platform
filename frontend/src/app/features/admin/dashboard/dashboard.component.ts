import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AdminIntelligenceResponse } from '../../../core/models/admin-intelligence.model';
import { AdminIntelligenceService } from '../../../core/services/admin-intelligence.service';
import { FormationService } from '../../../core/services/formation.service';
import { TrainerService } from '../../../core/services/trainer.service';
import { SessionService } from '../../../core/services/session.service';
import { EnrollmentService } from '../../../core/services/enrollment.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="page-header">
      <span class="eyebrow">Administration</span>
      <h1>Intelligence dashboard</h1>
      <p>Decision-support insights across users, skills, formations, sessions and enrollments.</p>
    </section>
    <div class="stats-grid">
      <article class="stat-card"><span>Total formations</span><strong>{{ stats().formations }}</strong></article>
      <article class="stat-card"><span>Total trainers</span><strong>{{ stats().trainers }}</strong></article>
      <article class="stat-card"><span>Total sessions</span><strong>{{ stats().sessions }}</strong></article>
      <article class="stat-card"><span>Total enrollments</span><strong>{{ stats().enrollments }}</strong></article>
    </div>

    <section class="page-header compact">
      <span class="eyebrow">Advanced analytics</span>
      <h2>Admin Intelligence Center</h2>
      <p>Automatic business alerts and recommended actions generated from the platform data.</p>
    </section>

    @if (loading()) {
      <article class="panel">
        <p class="muted">Loading intelligence center...</p>
      </article>
    } @else if (error()) {
      <article class="panel error-panel">
        <strong>Unable to load intelligence center</strong>
        <p>{{ error() }}</p>
      </article>
    } @else if (intelligence(); as dashboard) {
      <section class="grid two">
        <article class="panel health-panel">
          <span class="eyebrow">Global health score</span>
          <div class="health-score">
            <strong>{{ dashboard.globalHealthScore }}</strong>
            <span>/100</span>
          </div>
          <div class="progress-track large">
            <span [style.width.%]="dashboard.globalHealthScore"></span>
          </div>
          <p class="badge" [class.badge-success]="dashboard.globalHealthScore >= 80" [class.badge-warning]="dashboard.globalHealthScore >= 60 && dashboard.globalHealthScore < 80" [class.badge-danger]="dashboard.globalHealthScore < 60">
            {{ healthLabel(dashboard.globalHealthScore) }}
          </p>
        </article>

        <article class="panel">
          <h2>Recommended actions</h2>
          @for (action of dashboard.recommendedActions; track action.actionType + action.relatedEntityId + action.title) {
            <div class="list-row">
              <span class="badge" [class.badge-danger]="action.priority === 'HIGH'" [class.badge-warning]="action.priority === 'MEDIUM'" [class.badge-success]="action.priority === 'LOW'">{{ action.priority }}</span>
              <strong>{{ action.title }}</strong>
              <span>{{ action.description }}</span>
              <a class="btn btn-light" [routerLink]="actionRoute(action.actionType)">{{ action.actionLabel }}</a>
            </div>
          } @empty {
            <p class="muted">No recommended actions.</p>
          }
        </article>
      </section>

      <section class="card-grid intelligence-grid">
        <article class="item-card intelligence-card">
          <span class="eyebrow">Open sessions</span>
          <strong>{{ dashboard.summary.totalOpenSessions }}</strong>
          <p class="muted">OPEN or PLANNED</p>
        </article>
        <article class="item-card intelligence-card" [class.warning]="dashboard.summary.highRiskSessionCount > 0">
          <span class="eyebrow">High-risk sessions</span>
          <strong>{{ dashboard.summary.highRiskSessionCount }}</strong>
          <p class="muted">Almost full or full</p>
        </article>
        <article class="item-card intelligence-card" [class.critical]="dashboard.summary.overloadedTrainerCount > 0">
          <span class="eyebrow">Overloaded trainers</span>
          <strong>{{ dashboard.summary.overloadedTrainerCount }}</strong>
          <p class="muted">Above 35 hours</p>
        </article>
        <article class="item-card intelligence-card">
          <span class="eyebrow">High demand</span>
          <strong>{{ dashboard.summary.highDemandFormationCount }}</strong>
          <p class="muted">Formations needing attention</p>
        </article>
        <article class="item-card intelligence-card" [class.warning]="dashboard.summary.incompleteLearnerProfileCount > 0">
          <span class="eyebrow">Incomplete profiles</span>
          <strong>{{ dashboard.summary.incompleteLearnerProfileCount }}</strong>
          <p class="muted">Score below 60%</p>
        </article>
        <article class="item-card intelligence-card" [class.warning]="dashboard.summary.totalWaitlistedEnrollments > 0">
          <span class="eyebrow">Waitlisted learners</span>
          <strong>{{ dashboard.summary.totalWaitlistedEnrollments }}</strong>
          <p class="muted">Waiting for a place</p>
        </article>
      </section>

      <section class="grid two">
        <article class="panel">
          <h2>Smart alerts</h2>
          @for (alert of dashboard.alerts; track alert.type + alert.relatedEntityId + alert.message) {
            <div class="list-row">
              <span class="badge" [class.badge-danger]="alert.severity === 'CRITICAL'" [class.badge-warning]="alert.severity === 'WARNING'" [class.badge-success]="alert.severity === 'INFO'">{{ alert.severity }}</span>
              <strong>{{ alert.title }}</strong>
              <span>{{ alert.message }}</span>
              <small class="muted">{{ alert.actionLabel }}</small>
            </div>
          } @empty {
            <p class="muted">No alerts detected.</p>
          }
        </article>

        <article class="panel table-wrap">
          <h2>High-demand formations</h2>
          @for (item of dashboard.highDemandFormations; track item.formationId) {
            <div class="list-row">
              <span class="badge" [class.badge-danger]="item.demandScore >= 80" [class.badge-warning]="item.demandScore < 80">{{ item.demandScore }}</span>
              <strong>{{ item.formationTitle }}</strong>
              <span class="muted">{{ item.categoryName }}</span>
              <span>{{ item.reason }}</span>
              <a class="btn btn-light" routerLink="/admin/sessions">{{ item.suggestedAction }}</a>
            </div>
          } @empty {
            <p class="muted">No high-demand formations detected.</p>
          }
        </article>
      </section>

      <section class="grid two">
        <article class="panel table-wrap">
          <h2>Session risks</h2>
          <table>
            <thead><tr><th>Session</th><th>Risk</th><th>Capacity</th></tr></thead>
            <tbody>
              @for (item of dashboard.sessionRisks; track item.sessionId) {
                <tr>
                  <td>{{ item.sessionTitle }}<br><span class="muted">{{ item.formationTitle }}</span></td>
                  <td><span class="badge" [class.badge-danger]="item.riskLevel === 'FULL'" [class.badge-warning]="item.riskLevel === 'HIGH' || item.riskLevel === 'MEDIUM'">{{ item.riskLevel }}</span></td>
                  <td>
                    {{ item.confirmedEnrollments }}/{{ item.capacity }}
                    <div class="progress-track"><span [style.width.%]="item.capacityUsagePercentage > 100 ? 100 : item.capacityUsagePercentage"></span></div>
                    <span class="muted">{{ item.suggestedAction }}</span>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </article>
      </section>

      <section class="grid two">
        <article class="panel table-wrap">
          <h2>Trainer workload insights</h2>
          <table>
            <thead><tr><th>Trainer</th><th>Load</th><th>Action</th></tr></thead>
            <tbody>
              @for (item of dashboard.overloadedTrainers; track item.trainerId) {
                <tr>
                  <td>{{ item.trainerFullName }}<br><span class="muted">{{ item.trainerEmail }}</span><br><span class="muted">{{ item.sessionCount }} sessions</span></td>
                  <td><span class="badge" [class.badge-danger]="item.workloadLevel === 'OVERLOADED'" [class.badge-warning]="item.workloadLevel === 'HIGH'">{{ item.workloadLevel }}</span><br>{{ item.totalHours }}h</td>
                  <td>{{ item.suggestedAction }}</td>
                </tr>
              }
            </tbody>
          </table>
        </article>

        <article class="panel table-wrap">
          <h2>Top missing skills</h2>
          <table>
            <thead><tr><th>Skill</th><th>Missing</th><th>Action</th></tr></thead>
            <tbody>
              @for (item of dashboard.topMissingSkills; track item.skillId) {
                <tr>
                  <td>{{ item.skillName }}<br><span class="muted">{{ item.relatedFormationCount }} formations</span></td>
                  <td>{{ item.missingCount }} learners</td>
                  <td>{{ item.suggestedAction }}</td>
                </tr>
              }
            </tbody>
          </table>
        </article>
      </section>

      <section class="panel table-wrap">
        <h2>Learner profile risks</h2>
        <table>
          <thead><tr><th>Learner</th><th>Score</th><th>Missing fields</th><th>Action</th></tr></thead>
          <tbody>
            @for (item of dashboard.learnerProfileRisks; track item.learnerId) {
              <tr>
                <td>{{ item.learnerFullName }}</td>
                <td>{{ item.profileScore }}%</td>
                <td>{{ item.missingFields.join(', ') }}</td>
                <td>{{ item.suggestedAction }}</td>
              </tr>
            }
          </tbody>
        </table>
      </section>
    }
  `
})
export class AdminDashboardComponent implements OnInit {
  stats = signal({ formations: 0, trainers: 0, sessions: 0, enrollments: 0 });
  intelligence = signal<AdminIntelligenceResponse | null>(null);
  loading = signal(true);
  error = signal('');

  constructor(
    private readonly formations: FormationService,
    private readonly trainers: TrainerService,
    private readonly sessions: SessionService,
    private readonly enrollments: EnrollmentService,
    private readonly adminIntelligence: AdminIntelligenceService
  ) {}

  ngOnInit(): void {
    forkJoin({
      formations: this.formations.list({ size: 1 }),
      trainers: this.trainers.list(),
      sessions: this.sessions.list({ size: 1 })
    }).subscribe(({ formations, trainers, sessions }) => {
      this.stats.set({
        formations: formations.totalElements,
        trainers: trainers.length,
        sessions: sessions.totalElements,
        enrollments: 0
      });
    });
    this.adminIntelligence.getIntelligence().subscribe({
      next: (dashboard) => {
        this.intelligence.set(dashboard);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Check that the backend is running and that your account has ADMIN role.');
        this.loading.set(false);
      }
    });
  }

  healthLabel(score: number): string {
    if (score >= 80) return 'Healthy';
    if (score >= 60) return 'Needs attention';
    return 'Critical attention needed';
  }

  actionRoute(actionType: string): string {
    if (actionType === 'CREATE_SESSION' || actionType === 'REVIEW_SESSION_CAPACITY') return '/admin/sessions';
    if (actionType === 'REBALANCE_TRAINER') return '/admin/trainer-workload';
    if (actionType === 'CREATE_SKILL_CONTENT') return '/admin/formations';
    return '/admin/dashboard';
  }
}
