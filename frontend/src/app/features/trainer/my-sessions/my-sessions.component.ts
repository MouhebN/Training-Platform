import { Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Enrollment, TrainingSession } from '../../../core/models/session.model';
import { ChatService } from '../../../core/services/chat.service';
import { ConfirmDialogService } from '../../../core/services/confirm-dialog.service';
import { EnrollmentService } from '../../../core/services/enrollment.service';
import { SessionService } from '../../../core/services/session.service';
import { formatDateTime24 } from '../../../core/utils/date-time.util';

type SessionFilter = 'ALL' | 'UPCOMING' | 'LIVE' | 'COMPLETED';

interface FormationSessionGroup {
  formationId: number;
  formationTitle: string;
  sessionCount: number;
  completedSessions: number;
  progressPercentage: number;
  sessions: TrainingSession[];
}

@Component({
  selector: 'app-trainer-my-sessions',
  standalone: true,
  imports: [RouterLink],
  styles: [`
    :host { display: block; max-width: 1080px; }
    .filters {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      margin-bottom: 20px;
    }
    .filter-chip {
      appearance: none;
      border: 1px solid var(--line);
      background: #fff;
      color: var(--text);
      border-radius: 999px;
      padding: 8px 16px;
      font: inherit;
      font-size: 13px;
      cursor: pointer;
    }
    .filter-chip.active {
      border-color: rgba(230, 98, 57, .45);
      background: var(--primary-soft);
      color: var(--primary-dark);
      font-weight: 600;
    }
    .formation-stack { display: grid; gap: 22px; }
    .formation-block {
      background: #fff;
      border: 1px solid var(--line);
      border-radius: 14px;
      overflow: hidden;
    }
    .formation-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 16px;
      padding: 18px 20px;
      border-bottom: 1px solid var(--line);
      background: #fcfcfc;
    }
    .formation-header h2 { margin: 0 0 4px; font-size: 18px; }
    .formation-header p { margin: 0; color: var(--muted); font-size: 13px; }
    .formation-stats {
      display: grid;
      gap: 6px;
      min-width: 120px;
      text-align: right;
    }
    .formation-stats strong { font-size: 18px; }
    .formation-stats span { color: var(--muted); font-size: 12px; }
    .session-table { width: 100%; }
    .session-table-head,
    .session-table-row {
      display: grid;
      grid-template-columns: minmax(160px, 1.6fr) minmax(150px, 1.4fr) 90px 100px 110px minmax(180px, auto);
      gap: 12px;
      align-items: center;
      padding: 12px 20px;
    }
    .session-table-head {
      color: var(--muted);
      font-size: 11px;
      font-weight: 600;
      letter-spacing: .04em;
      text-transform: uppercase;
      border-bottom: 1px solid var(--line);
      background: #fafafa;
    }
    .session-table-row {
      border-bottom: 1px solid var(--line);
      font-size: 14px;
    }
    .session-table-row:last-child { border-bottom: none; }
    .session-table-row.live {
      background: rgba(0, 184, 219, .06);
    }
    .session-table-row.expanded {
      border-bottom: none;
      background: #fafafa;
    }
    .session-title { font-weight: 600; }
    .session-index {
      display: block;
      margin-top: 2px;
      color: var(--muted);
      font-size: 12px;
      font-weight: 400;
    }
    .session-when .meta { color: var(--muted); font-size: 12px; margin-top: 2px; }
    .row-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      justify-content: flex-end;
    }
    .row-actions .btn {
      min-height: 34px;
      padding: 7px 12px;
      font-size: 13px;
    }
    .attendance-panel {
      padding: 0 20px 18px;
      background: #fafafa;
      border-bottom: 1px solid var(--line);
    }
    .attendance-panel-inner {
      border: 1px solid var(--line);
      border-radius: 12px;
      background: #fff;
      padding: 16px 18px;
    }
    .attendance-panel h3 { margin: 0 0 6px; font-size: 15px; }
    .attendance-panel .muted { margin: 0 0 14px; }
    .attendance-panel table { margin-bottom: 14px; }
    .attendance-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
    }
    @media (max-width: 900px) {
      .session-table-head { display: none; }
      .session-table-row {
        grid-template-columns: 1fr;
        gap: 8px;
        padding: 16px 18px;
      }
      .row-actions { justify-content: flex-start; }
      .row-actions .btn { flex: 1 1 auto; }
    }
  `],
  template: `
    <section class="page-header">
      <h1>My sessions</h1>
      <p>Sessions grouped by formation — start live classes, take attendance, and finish séances.</p>
    </section>

    @if (message()) { <p class="alert success">{{ message() }}</p> }
    @if (error()) { <p class="alert error">{{ error() }}</p> }

    <div class="filters">
      @for (option of filterOptions; track option.value) {
        <button
          class="filter-chip"
          [class.active]="filter() === option.value"
          type="button"
          (click)="filter.set(option.value)">
          {{ option.label }}
        </button>
      }
    </div>

    <div class="formation-stack">
      @for (group of filteredGroups(); track group.formationId) {
        <section class="formation-block">
          <header class="formation-header">
            <div>
              <h2>{{ group.formationTitle }}</h2>
              <p>{{ group.completedSessions }} of {{ group.sessionCount }} sessions completed</p>
            </div>
            <div class="formation-stats">
              <strong>{{ group.progressPercentage }}%</strong>
              <span>{{ group.sessions.length }} assigned</span>
            </div>
          </header>

          <div class="session-table">
            <div class="session-table-head">
              <span>Session</span>
              <span>Schedule</span>
              <span>Learners</span>
              <span>Delivery</span>
              <span>Status</span>
              <span>Actions</span>
            </div>

            @for (session of group.sessions; track session.id; let index = $index) {
              <div
                class="session-table-row"
                [class.live]="session.status === 'IN_PROGRESS'"
                [class.expanded]="expandedSessionId() === session.id">
                <div>
                  <span class="session-title">{{ session.title }}</span>
                  <span class="session-index">Session {{ index + 1 }} of {{ group.sessionCount }}</span>
                </div>
                <div class="session-when">
                  <strong>{{ formatWhen(session.startDate) }}</strong>
                  <div class="meta">→ {{ formatWhen(session.endDate) }}</div>
                </div>
                <div>{{ session.enrolledCount }}/{{ session.capacity }}</div>
                <div>
                  {{ session.online ? 'Online' : 'Onsite' }}
                  @if (!session.online && session.location) {
                    <div class="meta">{{ session.location }}</div>
                  }
                </div>
                <div>
                  <span class="badge"
                    [class.badge-success]="session.status === 'COMPLETED'"
                    [class.badge-warning]="session.status === 'IN_PROGRESS' || session.status === 'OPEN'"
                    [class.badge-danger]="session.status === 'CANCELLED'">
                    {{ statusLabel(session.status) }}
                  </span>
                </div>
                <div class="row-actions">
                  @if (canStart(session)) {
                    <button class="btn btn-primary" type="button" (click)="start(session)">Start</button>
                  }
                  @if (session.online && session.status === 'IN_PROGRESS') {
                    <a class="btn btn-primary" [routerLink]="['/trainer/sessions', session.id, 'classroom']">Join live</a>
                  }
                  @if (!session.online && session.status !== 'CANCELLED') {
                    <button class="btn btn-light" type="button" (click)="toggleAttendance(session)">
                      {{ expandedSessionId() === session.id ? 'Close' : 'Attendance' }}
                    </button>
                  }
                  <a class="btn btn-light" [routerLink]="['/trainer/sessions', session.id, 'chat']">
                    Chat
                    @if (unreadCounts()[session.id]) {
                      <span class="badge badge-danger">{{ unreadCounts()[session.id] }}</span>
                    }
                  </a>
                </div>
              </div>

              @if (expandedSessionId() === session.id) {
                <div class="attendance-panel">
                  <div class="attendance-panel-inner">
                    <h3>Attendance · {{ session.title }}</h3>
                    <p class="muted">Mark present learners, then complete the session. They receive formation progress and skills.</p>
                    <table>
                      <thead><tr><th>Present</th><th>Learner</th><th>Status</th></tr></thead>
                      <tbody>
                        @for (enrollment of enrollments(); track enrollment.id) {
                          <tr>
                            <td>
                              <input
                                type="checkbox"
                                [disabled]="enrollment.status !== 'CONFIRMED' || session.status === 'COMPLETED'"
                                [checked]="presentIds().includes(enrollment.id)"
                                (change)="togglePresent(enrollment.id)">
                            </td>
                            <td>{{ enrollment.learnerFullName }}</td>
                            <td>{{ enrollment.status }}</td>
                          </tr>
                        } @empty {
                          <tr><td colspan="3" class="muted">No enrollments for this session.</td></tr>
                        }
                      </tbody>
                    </table>
                    <div class="attendance-actions">
                      @if (session.status !== 'COMPLETED' && session.status !== 'CANCELLED') {
                        <button class="btn btn-primary" type="button" (click)="complete(session)">Complete session</button>
                      }
                      <button class="btn btn-light" type="button" (click)="expandedSessionId.set(null)">Close</button>
                    </div>
                  </div>
                </div>
              }
            }
          </div>
        </section>
      } @empty {
        <section class="panel"><p class="muted">No sessions match this filter.</p></section>
      }
    </div>
  `
})
export class MySessionsComponent implements OnInit, OnDestroy {
  sessions = signal<TrainingSession[]>([]);
  enrollments = signal<Enrollment[]>([]);
  unreadCounts = signal<Record<number, number>>({});
  expandedSessionId = signal<number | null>(null);
  presentIds = signal<number[]>([]);
  message = signal('');
  error = signal('');
  filter = signal<SessionFilter>('ALL');
  filterOptions: { value: SessionFilter; label: string }[] = [
    { value: 'ALL', label: 'All formations' },
    { value: 'UPCOMING', label: 'Upcoming' },
    { value: 'LIVE', label: 'Live now' },
    { value: 'COMPLETED', label: 'Completed' }
  ];
  private refreshTimer?: number;

  groups = computed(() => this.buildGroups(this.sessions()));
  filteredGroups = computed(() => {
    const groups = this.groups();
    switch (this.filter()) {
      case 'UPCOMING':
        return groups
          .map((group) => ({ ...group, sessions: group.sessions.filter((session) => this.isUpcoming(session)) }))
          .filter((group) => group.sessions.length > 0);
      case 'LIVE':
        return groups
          .map((group) => ({ ...group, sessions: group.sessions.filter((session) => session.status === 'IN_PROGRESS') }))
          .filter((group) => group.sessions.length > 0);
      case 'COMPLETED':
        return groups
          .map((group) => ({ ...group, sessions: group.sessions.filter((session) => session.status === 'COMPLETED') }))
          .filter((group) => group.sessions.length > 0);
      default:
        return groups;
    }
  });

  constructor(
    private readonly sessionsService: SessionService,
    private readonly enrollmentsService: EnrollmentService,
    private readonly chatService: ChatService,
    private readonly confirmDialog: ConfirmDialogService
  ) {}

  ngOnInit(): void {
    this.loadSessions();
    this.refreshTimer = window.setInterval(() => this.loadSessions(false), 10000);
  }

  ngOnDestroy(): void {
    window.clearInterval(this.refreshTimer);
  }

  loadSessions(showUnread = true): void {
    this.sessionsService.mineAsTrainer().subscribe((data) => {
      this.sessions.set(data);
      if (!showUnread) return;
      data.forEach((session) => this.chatService.unreadCount(session.id).subscribe((count) => {
        this.unreadCounts.update((counts) => ({ ...counts, [session.id]: count.unreadCount }));
      }));
    });
  }

  toggleAttendance(session: TrainingSession): void {
    if (this.expandedSessionId() === session.id) {
      this.expandedSessionId.set(null);
      return;
    }
    this.expandedSessionId.set(session.id);
    this.enrollmentsService.bySession(session.id).subscribe((data) => {
      this.enrollments.set(data);
      this.presentIds.set(data.filter((enrollment) => enrollment.status === 'CONFIRMED').map((enrollment) => enrollment.id));
    });
  }

  togglePresent(enrollmentId: number): void {
    const current = this.presentIds();
    this.presentIds.set(
      current.includes(enrollmentId) ? current.filter((id) => id !== enrollmentId) : [...current, enrollmentId]
    );
  }

  canStart(session: TrainingSession): boolean {
    return session.status === 'PLANNED' || session.status === 'OPEN';
  }

  isUpcoming(session: TrainingSession): boolean {
    return session.status === 'PLANNED' || session.status === 'OPEN';
  }

  start(session: TrainingSession): void {
    this.sessionsService.start(session.id).subscribe({
      next: (updated) => {
        this.message.set(`"${updated.title}" started. Enrolled learners were notified.`);
        this.error.set('');
        this.replaceSession(updated);
      },
      error: () => this.error.set('Could not start this session.')
    });
  }

  async complete(session: TrainingSession): Promise<void> {
    const ok = await this.confirmDialog.confirm({
      title: 'Complete session?',
      message: `Complete “${session.title}”? Present learners will be marked completed.`,
      confirmLabel: 'Complete',
      tone: 'primary'
    });
    if (!ok) return;
    this.sessionsService.complete(session.id, this.presentIds()).subscribe({
      next: (updated) => {
        this.message.set('Session completed. Present learners now have progress and formation skills.');
        this.error.set('');
        this.replaceSession(updated);
        this.enrollmentsService.bySession(session.id).subscribe((data) => this.enrollments.set(data));
      },
      error: () => this.error.set('Could not complete this session.')
    });
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'IN_PROGRESS': return 'Live';
      case 'PLANNED': return 'Planned';
      case 'OPEN': return 'Open';
      case 'COMPLETED': return 'Done';
      case 'CANCELLED': return 'Cancelled';
      default: return status;
    }
  }

  formatWhen(value?: string): string {
    return formatDateTime24(value);
  }

  private replaceSession(updated: TrainingSession): void {
    this.sessions.update((sessions) => sessions.map((session) => session.id === updated.id ? updated : session));
  }

  private buildGroups(sessions: TrainingSession[]): FormationSessionGroup[] {
    const map = new Map<number, FormationSessionGroup>();

    for (const session of sessions) {
      const existing = map.get(session.formationId);
      if (existing) {
        existing.sessions.push(session);
        continue;
      }
      map.set(session.formationId, {
        formationId: session.formationId,
        formationTitle: session.formationTitle,
        sessionCount: session.formationSessionCount && session.formationSessionCount > 0
          ? session.formationSessionCount
          : 1,
        completedSessions: 0,
        progressPercentage: 0,
        sessions: [session]
      });
    }

    return Array.from(map.values())
      .map((group) => {
        group.sessions.sort((left, right) =>
          new Date(left.startDate).getTime() - new Date(right.startDate).getTime()
        );
        const completedSessions = group.sessions.filter((session) => session.status === 'COMPLETED').length;
        const sessionCount = Math.max(group.sessionCount, group.sessions.length);
        const progressPercentage = Math.min(100, Math.round((completedSessions / sessionCount) * 100));
        return {
          ...group,
          sessionCount,
          completedSessions,
          progressPercentage
        };
      })
      .sort((left, right) => left.formationTitle.localeCompare(right.formationTitle));
  }
}
