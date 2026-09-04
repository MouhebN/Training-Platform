import { Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Enrollment } from '../../../core/models/session.model';
import { ChatService } from '../../../core/services/chat.service';
import { ConfirmDialogService } from '../../../core/services/confirm-dialog.service';
import { EnrollmentService } from '../../../core/services/enrollment.service';
import { formatDateTime24 } from '../../../core/utils/date-time.util';

type EnrollmentFilter = 'ALL' | 'ACTIVE' | 'COMPLETED';

interface FormationEnrollmentGroup {
  formationId: number;
  formationTitle: string;
  sessionCount: number;
  completedSessions: number;
  progressPercentage: number;
  enrollments: Enrollment[];
}

@Component({
  selector: 'app-my-enrollments',
  standalone: true,
  imports: [RouterLink],
  styles: [`
    :host { display: block; max-width: 980px; }
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
    .formation-stack { display: grid; gap: 18px; }
    .formation-card {
      display: grid;
      gap: 16px;
      padding: 22px 24px;
    }
    .formation-top {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 16px;
    }
    .formation-top h2 { margin: 0 0 6px; font-size: 20px; }
    .formation-top p { margin: 0; color: var(--muted); }
    .formation-progress { display: grid; gap: 8px; }
    .formation-progress span {
      font-size: 13px;
      color: var(--muted);
    }
    .session-list {
      display: grid;
      gap: 12px;
      border-top: 1px solid var(--line);
      padding-top: 16px;
    }
    .session-row {
      display: grid;
      gap: 10px;
      padding: 14px 16px;
      border: 1px solid var(--line);
      border-radius: 12px;
      background: #fafafa;
    }
    .session-row-top {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 12px;
    }
    .session-row-top h3 { margin: 0; font-size: 16px; }
    .session-meta { display: grid; gap: 4px; }
    .session-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      align-items: center;
    }
    @media (max-width: 640px) {
      .formation-top { flex-direction: column; }
      .session-actions .btn { width: 100%; }
    }
  `],
  template: `
    <section class="page-header">
      <h1>My enrollments</h1>
      <p>Grouped by formation — open a session to chat or join the live class.</p>
    </section>

    @if (message()) { <p class="alert success">{{ message() }}</p> }

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
        <article class="item-card formation-card">
          <div class="formation-top">
            <div>
              <h2>{{ group.formationTitle }}</h2>
              <p>{{ group.completedSessions }} of {{ group.sessionCount }} sessions completed</p>
            </div>
            <span class="badge"
              [class.badge-success]="group.progressPercentage === 100"
              [class.badge-warning]="group.progressPercentage > 0 && group.progressPercentage < 100">
              {{ group.progressPercentage }}%
            </span>
          </div>

          <div class="formation-progress">
            <div class="progress-track"><span [style.width.%]="group.progressPercentage"></span></div>
            <span>{{ group.enrollments.length }} session{{ group.enrollments.length === 1 ? '' : 's' }} enrolled</span>
          </div>

          <div class="session-list">
            @for (enrollment of group.enrollments; track enrollment.id) {
              <section class="session-row">
                <div class="session-row-top">
                  <h3>{{ enrollment.sessionTitle }}</h3>
                  <span class="badge"
                    [class.badge-warning]="enrollment.status === 'WAITLISTED'"
                    [class.badge-success]="enrollment.status === 'CONFIRMED' || enrollment.status === 'COMPLETED'">
                    {{ statusLabel(enrollment) }}
                  </span>
                </div>
                <div class="session-meta">
                  <div class="meta">Trainer: {{ enrollment.trainerFullName }}</div>
                  <div class="meta">{{ formatWhen(enrollment.sessionStartDate) }} → {{ formatWhen(enrollment.sessionEndDate) }}</div>
                  <div class="meta">{{ place(enrollment) }}</div>
                  @if (attendanceNote(enrollment)) {
                    <div class="meta">{{ attendanceNote(enrollment) }}</div>
                  }
                </div>
                <div class="session-actions">
                  @if (canJoinClassroom(enrollment)) {
                    <a class="btn btn-primary" [routerLink]="['/learner/sessions', enrollment.sessionId, 'classroom']">
                      Join live session
                    </a>
                  }
                  <a class="btn btn-light" [routerLink]="['/learner/sessions', enrollment.sessionId, 'chat']">
                    Chat
                    @if (unreadCounts()[enrollment.sessionId]) {
                      <span class="badge badge-danger">{{ unreadCounts()[enrollment.sessionId] }}</span>
                    }
                  </a>
                  @if (enrollment.status === 'CONFIRMED' || enrollment.status === 'WAITLISTED') {
                    <button class="btn btn-danger" type="button" (click)="cancel(enrollment)">Cancel</button>
                  }
                </div>
              </section>
            }
          </div>
        </article>
      } @empty {
        <section class="panel"><p class="muted">No enrollments match this filter.</p></section>
      }
    </div>
  `
})
export class MyEnrollmentsComponent implements OnInit, OnDestroy {
  enrollments = signal<Enrollment[]>([]);
  unreadCounts = signal<Record<number, number>>({});
  message = signal('');
  filter = signal<EnrollmentFilter>('ALL');
  filterOptions: { value: EnrollmentFilter; label: string }[] = [
    { value: 'ALL', label: 'All formations' },
    { value: 'ACTIVE', label: 'In progress' },
    { value: 'COMPLETED', label: 'Completed' }
  ];
  private refreshTimer?: number;

  groups = computed(() => this.buildGroups(this.enrollments()));
  filteredGroups = computed(() => {
    const groups = this.groups();
    switch (this.filter()) {
      case 'ACTIVE':
        return groups.filter((group) => group.progressPercentage < 100);
      case 'COMPLETED':
        return groups.filter((group) => group.progressPercentage === 100);
      default:
        return groups;
    }
  });

  constructor(
    private readonly service: EnrollmentService,
    private readonly chatService: ChatService,
    private readonly confirmDialog: ConfirmDialogService
  ) {}

  ngOnInit(): void {
    this.load();
    this.refreshTimer = window.setInterval(() => this.load(false), 10000);
  }

  ngOnDestroy(): void {
    window.clearInterval(this.refreshTimer);
  }

  load(showUnread = true): void {
    this.service.mine().subscribe((data) => {
      this.enrollments.set(data);
      if (!showUnread) return;
      data.forEach((enrollment) => this.chatService.unreadCount(enrollment.sessionId).subscribe((count) => {
        this.unreadCounts.update((counts) => ({ ...counts, [enrollment.sessionId]: count.unreadCount }));
      }));
    });
  }

  async cancel(enrollment: Enrollment): Promise<void> {
    const ok = await this.confirmDialog.confirm({
      title: 'Cancel enrollment?',
      message: `Cancel enrollment for “${enrollment.sessionTitle}”?`,
      confirmLabel: 'Cancel enrollment',
      tone: 'danger'
    });
    if (!ok) return;
    this.service.cancel(enrollment.id).subscribe((response) => {
      this.message.set(response.message);
      this.load();
    });
  }

  formatWhen(value?: string): string {
    return formatDateTime24(value);
  }

  statusLabel(enrollment: Enrollment): string {
    if (enrollment.status === 'COMPLETED' && enrollment.virtualAttendanceQualified) {
      return 'Present';
    }
    return enrollment.status;
  }

  place(enrollment: Enrollment): string {
    if (enrollment.online) {
      if (enrollment.sessionStatus === 'IN_PROGRESS') return 'Live video session';
      if (enrollment.sessionStatus === 'COMPLETED') return 'Online · finished';
      return 'Online · starts when trainer goes live';
    }
    return enrollment.location ? `Onsite · ${enrollment.location}` : 'Onsite';
  }

  attendanceNote(enrollment: Enrollment): string | null {
    if (!enrollment.online || enrollment.status !== 'COMPLETED') return null;
    if (enrollment.virtualAttendanceQualified) return 'Marked present for this session.';
    if (enrollment.virtualAttendancePercentage != null) return 'Did not meet attendance requirement.';
    return null;
  }

  canJoinClassroom(enrollment: Enrollment): boolean {
    return enrollment.online === true
      && enrollment.status === 'CONFIRMED'
      && enrollment.sessionStatus === 'IN_PROGRESS';
  }

  private buildGroups(enrollments: Enrollment[]): FormationEnrollmentGroup[] {
    const map = new Map<number, FormationEnrollmentGroup>();

    for (const enrollment of enrollments) {
      const formationId = enrollment.formationId;
      const existing = map.get(formationId);
      if (existing) {
        existing.enrollments.push(enrollment);
        continue;
      }
      map.set(formationId, {
        formationId,
        formationTitle: enrollment.formationTitle,
        sessionCount: enrollment.formationSessionCount && enrollment.formationSessionCount > 0
          ? enrollment.formationSessionCount
          : 1,
        completedSessions: 0,
        progressPercentage: 0,
        enrollments: [enrollment]
      });
    }

    return Array.from(map.values())
      .map((group) => {
        group.enrollments.sort((left, right) =>
          new Date(left.sessionStartDate || 0).getTime() - new Date(right.sessionStartDate || 0).getTime()
        );
        const completedSessions = group.enrollments.filter((enrollment) => enrollment.status === 'COMPLETED').length;
        const sessionCount = Math.max(group.sessionCount, group.enrollments.length);
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
