import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Enrollment, TrainingSession } from '../../../core/models/session.model';
import { ConfirmDialogService } from '../../../core/services/confirm-dialog.service';
import { EnrollmentService } from '../../../core/services/enrollment.service';
import { SessionService } from '../../../core/services/session.service';

@Component({
  selector: 'app-admin-enrollments',
  standalone: true,
  imports: [FormsModule],
  template: `
    <section class="page-header"><h1>Enrollments</h1><p>Review registrations per session.</p></section>
    @if (message()) { <p class="alert success">{{ message() }}</p> }
    <div class="panel">
      <select [(ngModel)]="sessionId" (change)="loadEnrollments()">
        <option [ngValue]="0">Choose session</option>
        @for (session of sessions(); track session.id) { <option [ngValue]="session.id">{{ session.title }}</option> }
      </select>
    </div>
    <div class="panel table-wrap">
      <table>
        <thead><tr><th>Learner</th><th>Formation</th><th>Status</th><th></th></tr></thead>
        <tbody>
          @for (enrollment of enrollments(); track enrollment.id) {
            <tr>
              <td>{{ enrollment.learnerFullName }}</td>
              <td>{{ enrollment.formationTitle }}</td>
              <td><span class="badge" [class.badge-warning]="enrollment.status === 'WAITLISTED'" [class.badge-success]="enrollment.status === 'CONFIRMED'">{{ enrollment.status }}</span></td>
              <td>
                @if (enrollment.status === 'WAITLISTED') {
                  <button class="btn btn-primary" (click)="setStatus(enrollment, 'CONFIRMED')">Approve</button>
                }
                <button class="btn btn-light" (click)="setStatus(enrollment, 'COMPLETED')">Complete</button>
                <button class="btn btn-danger" (click)="cancel(enrollment)">Cancel</button>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `
})
export class EnrollmentsComponent implements OnInit {
  sessions = signal<TrainingSession[]>([]);
  enrollments = signal<Enrollment[]>([]);
  message = signal('');
  sessionId = 0;
  constructor(
    private readonly sessionService: SessionService,
    private readonly enrollmentService: EnrollmentService,
    private readonly confirmDialog: ConfirmDialogService
  ) {}
  ngOnInit(): void { this.sessionService.list({ size: 100 }).subscribe((p) => this.sessions.set(p.content)); }
  loadEnrollments(): void { if (this.sessionId) this.enrollmentService.bySession(this.sessionId).subscribe((d) => this.enrollments.set(d)); }
  setStatus(enrollment: Enrollment, status: string): void { this.enrollmentService.updateStatus(enrollment.id, status).subscribe(() => this.loadEnrollments()); }
  async cancel(enrollment: Enrollment): Promise<void> {
    const ok = await this.confirmDialog.confirm({
      title: 'Cancel enrollment?',
      message: `Cancel enrollment for “${enrollment.learnerFullName}” on “${enrollment.formationTitle}”?`,
      confirmLabel: 'Cancel enrollment',
      tone: 'danger'
    });
    if (!ok) return;
    this.enrollmentService.cancel(enrollment.id).subscribe((response) => {
      this.message.set(response.message);
      this.loadEnrollments();
    });
  }
}
