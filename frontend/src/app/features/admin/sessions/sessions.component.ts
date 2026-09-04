import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { startWith } from 'rxjs';
import { Formation } from '../../../core/models/catalogue.model';
import { SessionConflictCheckResponse, SessionPlanningSuggestion } from '../../../core/models/planning.model';
import { Trainer, TrainerAvailability } from '../../../core/models/profile.model';
import { TrainingSession } from '../../../core/models/session.model';
import { FormationService } from '../../../core/services/formation.service';
import { ConfirmDialogService } from '../../../core/services/confirm-dialog.service';
import { PlanningService } from '../../../core/services/planning.service';
import { SessionService } from '../../../core/services/session.service';
import { TrainerService } from '../../../core/services/trainer.service';
import { formatTime24, toApiDateTime } from '../../../core/utils/date-time.util';

@Component({
  selector: 'app-admin-sessions',
  standalone: true,
  imports: [ReactiveFormsModule],
  styles: [`
    .availability-list { display: flex; flex-wrap: wrap; gap: 8px; }
    .availability-list .badge { margin: 0; padding: 7px 11px; }
  `],
  template: `
    <section class="page-header"><h1>Sessions</h1><p>Plan, start, reschedule or cancel training sessions. Learners are reminded automatically.</p></section>
    @if (message()) { <p class="alert success">{{ message() }}</p> }
    @if (error()) { <p class="alert error">{{ error() }}</p> }
    <form class="panel form-grid" [formGroup]="form" (ngSubmit)="save()">
      <label>Title
        <input placeholder="Session title" formControlName="title">
      </label>
      <label>Formation
        <select formControlName="formationId">
          <option [ngValue]="0">Choose formation</option>
          @for (f of formations(); track f.id) { <option [ngValue]="f.id">{{ f.title }} ({{ f.sessionCount }} sessions)</option> }
        </select>
      </label>
      @if (selectedFormation(); as formation) {
        @if (isFormationAtCapacity(formation.id)) {
          <p class="alert warning-alert span-full">
            All {{ formation.sessionCount }} sessions are already scheduled for this formation.
            Cancel or delete a session before creating another.
          </p>
        } @else {
          <p class="muted span-full">
            {{ formation.sessionCount }} sessions planned for this formation · {{ scheduledSessionsFor(formation.id) }} already scheduled.
            Create one session at a time (session {{ scheduledSessionsFor(formation.id) + 1 }} of {{ formation.sessionCount }}).
          </p>
        }
      }
      <label>Trainer
        <select formControlName="trainerId">
          <option [ngValue]="0">Choose trainer</option>
          @for (t of trainers(); track t.id) { <option [ngValue]="t.id">{{ t.user.firstName }} {{ t.user.lastName }}</option> }
        </select>
      </label>
      <label>Start date
        <input type="date" formControlName="startDate">
      </label>
      <label>Start hour (24-hour)
        <select formControlName="startTime">
          @for (time of timeOptions; track time) { <option [value]="time">{{ time }}</option> }
        </select>
      </label>
      <label>End date
        <input type="date" formControlName="endDate">
      </label>
      <label>End hour (24-hour)
        <select formControlName="endTime">
          @for (time of timeOptions; track time) { <option [value]="time">{{ time }}</option> }
        </select>
      </label>
      <label>Capacity (seats)
        <input type="number" min="1" step="1" formControlName="capacity">
      </label>
      <label>Status
        <select formControlName="status">
          <option>PLANNED</option>
          <option>OPEN</option>
          <option>IN_PROGRESS</option>
          <option>COMPLETED</option>
          <option>CANCELLED</option>
        </select>
      </label>
      <label>Delivery
        <select formControlName="online">
          <option [ngValue]="true">Online</option>
          <option [ngValue]="false">Onsite</option>
        </select>
      </label>
      @if (form.controls.online.value) {
        <p class="muted">The meeting link can be shared with learners later in the session chat.</p>
      } @else {
        <label>Location
          <input placeholder="Room, building or city" formControlName="location">
        </label>
      }
      <label class="span-full">Description
        <textarea placeholder="Optional description" formControlName="description"></textarea>
      </label>
      <div class="actions">
        <button type="button" class="btn btn-light" [disabled]="!canSuggest()" (click)="suggest()">Suggest best trainers/time slots</button>
        <button type="button" class="btn btn-light" [disabled]="!canCheckConflicts()" (click)="checkConflicts()">Check conflicts</button>
        <button class="btn btn-primary" [disabled]="!canSave()" type="submit">{{ editingId() ? 'Save changes' : 'Create session' }}</button>
        @if (editingId()) {
          <button type="button" class="btn btn-light" (click)="cancelEdit()">Cancel edit</button>
        }
      </div>
    </form>
    @if (form.controls.trainerId.value > 0) {
      <section class="panel">
        <h2>Selected trainer availability</h2>
        @if (selectedAvailability().length) {
          <p class="muted">Choose a same-day session that fits completely inside one of these slots:</p>
          <div class="availability-list">
            @for (slot of selectedAvailability(); track slot.id) {
              <span class="badge">{{ dayLabel(slot.dayOfWeek) }} {{ formatTime(slot.startTime) }}–{{ formatTime(slot.endTime) }}</span>
            }
          </div>
        } @else {
          <p class="alert warning-alert">This trainer has not published any weekly availability.</p>
        }
      </section>
    }
    @if (conflictResult(); as result) {
      <section class="panel">
        <h2>Conflict detection</h2>
        @if (!result.conflicts.length) {
          <p class="alert success">No conflict detected.</p>
        }
        @for (conflict of result.conflicts; track conflict.type + conflict.message) {
          <p class="alert" [class.error]="conflict.severity === 'BLOCKING'" [class.success]="false" [class.warning-alert]="conflict.severity === 'WARNING'">
            <strong>{{ conflict.severity }}</strong> {{ conflict.type }} - {{ conflict.message }}
            @if (conflict.relatedSessionTitle) { <span>({{ conflict.relatedSessionTitle }})</span> }
          </p>
        }
      </section>
    }
    @if (suggestions().length) {
      <section class="card-grid">
        @for (suggestion of suggestions(); track suggestion.trainerId + suggestion.suggestedStartDate) {
          <article class="item-card">
            <div class="inline spread">
              <h3>{{ suggestion.trainerFullName }}</h3>
              <span class="badge badge-success">Score {{ suggestion.score }}</span>
            </div>
            <div class="meta">{{ suggestion.trainerEmail }}</div>
            <p><strong>{{ suggestion.suggestedStartDate }}</strong><br>{{ suggestion.suggestedEndDate }}</p>
            <p>
              <span class="badge">{{ suggestion.workloadLevel }}</span>
              <span class="badge badge-success">{{ suggestion.expertiseMatchPercentage }}% expertise</span>
            </p>
            <ul class="reason-list">
              @for (reason of suggestion.reasons; track reason) { <li>{{ reason }}</li> }
            </ul>
            @if (suggestion.warnings.length) {
              <p class="muted">Warnings: {{ suggestion.warnings.join(', ') }}</p>
            }
            <button class="btn btn-primary" type="button" (click)="useSuggestion(suggestion)">Use this suggestion</button>
          </article>
        }
      </section>
    }
    <div class="panel table-wrap">
      <table>
        <thead><tr><th>Session</th><th>Trainer</th><th>Status</th><th>Places</th><th></th></tr></thead>
        <tbody>
          @for (session of sessions(); track session.id) {
            <tr>
              <td>{{ session.title }}<br><span class="muted">{{ session.formationTitle }} · {{ session.online ? 'Online' : session.location }}</span></td>
              <td>{{ session.trainerFullName }}</td>
              <td>{{ session.status }}</td>
              <td>{{ session.availablePlaces }}/{{ session.capacity }}</td>
              <td>
                <div class="actions">
                  <button class="btn btn-light" type="button" (click)="edit(session)">Edit</button>
                  @if (canStart(session)) {
                    <button class="btn btn-primary" type="button" (click)="start(session)">Start</button>
                  }
                  @if (canCancel(session)) {
                    <button class="btn btn-light" type="button" (click)="cancel(session)">Cancel</button>
                  }
                  <button class="btn btn-danger" type="button" (click)="remove(session)">Delete</button>
                </div>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `
})
export class SessionsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  readonly timeOptions = Array.from({ length: 24 }, (_, hour) => `${hour.toString().padStart(2, '0')}:00`);
  formations = signal<Formation[]>([]);
  trainers = signal<Trainer[]>([]);
  sessions = signal<TrainingSession[]>([]);
  suggestions = signal<SessionPlanningSuggestion[]>([]);
  conflictResult = signal<SessionConflictCheckResponse | null>(null);
  selectedAvailability = signal<TrainerAvailability[]>([]);
  editingId = signal<number | null>(null);
  message = signal('');
  error = signal('');
  form = this.fb.nonNullable.group({
    formationId: [0, [Validators.required, Validators.min(1)]],
    trainerId: [0, [Validators.required, Validators.min(1)]],
    title: ['', Validators.required],
    description: [''],
    startDate: ['', Validators.required],
    startTime: ['09:00', Validators.required],
    endDate: ['', Validators.required],
    endTime: ['17:00', Validators.required],
    capacity: [10, [Validators.required, Validators.min(1)]],
    location: [''],
    online: [true],
    status: ['OPEN']
  });

  constructor(
    private readonly sessionService: SessionService,
    private readonly formationService: FormationService,
    private readonly trainerService: TrainerService,
    private readonly planningService: PlanningService,
    private readonly confirmDialog: ConfirmDialogService
  ) {}
  ngOnInit(): void {
    this.formationService.list({ size: 100 }).subscribe((p) => this.formations.set(p.content));
    this.trainerService.list().subscribe((d) => this.trainers.set(d));
    this.form.controls.trainerId.valueChanges.pipe(
      startWith(this.form.controls.trainerId.value),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((trainerId) => this.loadAvailability(trainerId));
    this.form.controls.online.valueChanges.pipe(
      startWith(this.form.controls.online.value),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((online) => this.applyDeliveryMode(online));
    this.load();
  }
  load(): void { this.sessionService.list({ size: 100 }).subscribe((p) => this.sessions.set(p.content)); }
  loadAvailability(trainerId: number): void {
    if (!trainerId) {
      this.selectedAvailability.set([]);
      return;
    }
    this.trainerService.availability(trainerId).subscribe((slots) => this.selectedAvailability.set(
      [...slots].sort((left, right) => this.dayOrder(left.dayOfWeek) - this.dayOrder(right.dayOfWeek)
        || left.startTime.localeCompare(right.startTime))
    ));
  }
  dayLabel(day: string): string {
    return day.charAt(0) + day.slice(1).toLowerCase();
  }
  formatTime(value: string): string {
    return formatTime24(value);
  }
  private dayOrder(day: string): number {
    return ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'].indexOf(day);
  }
  canSuggest(): boolean {
    const value = this.form.getRawValue();
    return value.formationId > 0 && !!value.startDate && !!value.endDate;
  }
  canCheckConflicts(): boolean {
    const value = this.form.getRawValue();
    return value.formationId > 0 && value.trainerId > 0 && !!value.startDate && !!value.endDate && (value.online || !!value.location.trim());
  }
  canSave(): boolean {
    if (!this.form.valid || this.conflictResult()?.hasBlockingConflicts) {
      return false;
    }
    if (this.editingId()) {
      return true;
    }
    const formationId = this.form.controls.formationId.value;
    return formationId > 0 && !this.isFormationAtCapacity(formationId);
  }
  canStart(session: TrainingSession): boolean {
    return session.status === 'PLANNED' || session.status === 'OPEN';
  }
  canCancel(session: TrainingSession): boolean {
    return session.status !== 'COMPLETED' && session.status !== 'CANCELLED';
  }
  selectedFormation(): Formation | undefined {
    const formationId = this.form.controls.formationId.value;
    return this.formations().find((formation) => formation.id === formationId);
  }
  scheduledSessionsFor(formationId: number): number {
    return this.sessions().filter((session) => session.formationId === formationId && session.status !== 'CANCELLED').length;
  }
  isFormationAtCapacity(formationId: number): boolean {
    const formation = this.formations().find((item) => item.id === formationId);
    if (!formation) return false;
    return this.scheduledSessionsFor(formationId) >= formation.sessionCount;
  }
  private applyDeliveryMode(online: boolean): void {
    const location = this.form.controls.location;
    if (online) {
      location.setValue('', { emitEvent: false });
      location.clearValidators();
    } else {
      location.setValidators([Validators.required]);
    }
    location.updateValueAndValidity({ emitEvent: false });
  }
  save(): void {
    const payload = this.payload();
    const editingId = this.editingId();
    const request = editingId
      ? this.sessionService.update(editingId, payload)
      : this.sessionService.create(payload);
    request.subscribe({
      next: (saved) => {
        this.message.set(editingId
          ? `"${saved.title}" updated. Learners were notified if the schedule or place changed.`
          : `"${saved.title}" created.`);
        this.resetForm();
        this.load();
      },
      error: (err) => {
        const apiMessage = err?.error?.message;
        this.error.set(apiMessage ?? (editingId
          ? 'Could not update session. Check planning conflicts, dates and location.'
          : 'Could not create session. Check planning conflicts, dates and location.'));
      }
    });
  }
  edit(session: TrainingSession): void {
    this.editingId.set(session.id);
    this.form.patchValue({
      formationId: session.formationId,
      trainerId: session.trainerId,
      title: session.title,
      description: session.description ?? '',
      startDate: this.datePart(session.startDate),
      startTime: this.timePart(session.startDate),
      endDate: this.datePart(session.endDate),
      endTime: this.timePart(session.endDate),
      capacity: session.capacity,
      location: session.location ?? '',
      online: session.online,
      status: session.status
    });
    this.conflictResult.set(null);
    this.message.set(`Editing "${session.title}". Change the time or place to notify enrolled learners.`);
    this.error.set('');
  }
  cancelEdit(): void {
    this.resetForm();
  }
  start(session: TrainingSession): void {
    this.sessionService.start(session.id).subscribe({
      next: (updated) => {
        this.message.set(`"${updated.title}" started. Enrolled learners were notified.`);
        this.error.set('');
        this.load();
      },
      error: () => this.error.set('Could not start this session.')
    });
  }
  async cancel(session: TrainingSession): Promise<void> {
    const ok = await this.confirmDialog.confirm({
      title: 'Cancel session?',
      message: `Cancel “${session.title}”? Enrolled learners and the trainer will be notified.`,
      confirmLabel: 'Cancel session',
      tone: 'warning'
    });
    if (!ok) return;
    this.sessionService.cancel(session.id).subscribe({
      next: (updated) => {
        this.message.set(`"${updated.title}" cancelled.`);
        this.error.set('');
        if (this.editingId() === updated.id) this.resetForm();
        this.load();
      },
      error: () => this.error.set('Could not cancel this session.')
    });
  }
  async remove(session: TrainingSession): Promise<void> {
    const ok = await this.confirmDialog.confirm({
      title: 'Delete session?',
      message: `Delete session “${session.title}”? This removes it permanently.`,
      confirmLabel: 'Delete',
      tone: 'danger'
    });
    if (!ok) return;
    this.sessionService.delete(session.id).subscribe({
      next: () => {
        this.message.set(`"${session.title}" deleted.`);
        this.error.set('');
        if (this.editingId() === session.id) this.resetForm();
        this.load();
      },
      error: () => this.error.set('Could not delete this session.')
    });
  }
  private payload() {
    const value = this.form.getRawValue();
    return {
      formationId: value.formationId,
      trainerId: value.trainerId,
      title: value.title,
      description: value.description,
      startDate: this.dateTime(value.startDate, value.startTime),
      endDate: this.dateTime(value.endDate, value.endTime),
      capacity: value.capacity,
      location: value.online ? null : value.location.trim(),
      online: value.online,
      meetingUrl: null,
      status: value.status
    };
  }
  private resetForm(): void {
    this.editingId.set(null);
    this.form.reset({
      formationId: 0, trainerId: 0, title: '', description: '',
      startDate: '', startTime: '09:00', endDate: '', endTime: '17:00',
      capacity: 10, location: '', online: true, status: 'OPEN'
    });
    this.suggestions.set([]);
    this.conflictResult.set(null);
    this.error.set('');
  }
  suggest(): void {
    const value = this.form.getRawValue();
    this.planningService.suggestions({
      formationId: value.formationId,
      preferredStartDate: this.dateOnly(value.startDate),
      preferredEndDate: this.dateOnly(value.endDate),
      durationHours: this.durationHours(
        this.dateTime(value.startDate, value.startTime),
        this.dateTime(value.endDate, value.endTime)
      ),
      online: value.online,
      preferredCapacity: value.capacity
    }).subscribe((suggestions) => this.suggestions.set(suggestions));
  }
  checkConflicts(): void {
    const value = this.form.getRawValue();
    this.planningService.conflicts({
      formationId: value.formationId,
      trainerId: value.trainerId,
      startDate: this.dateTime(value.startDate, value.startTime),
      endDate: this.dateTime(value.endDate, value.endTime),
      online: value.online,
      location: value.online ? undefined : value.location
    }).subscribe((result) => this.conflictResult.set(result));
  }
  useSuggestion(suggestion: SessionPlanningSuggestion): void {
    this.form.patchValue({
      trainerId: suggestion.trainerId,
      startDate: this.datePart(suggestion.suggestedStartDate),
      startTime: this.timePart(suggestion.suggestedStartDate),
      endDate: this.datePart(suggestion.suggestedEndDate),
      endTime: this.timePart(suggestion.suggestedEndDate)
    });
    this.checkConflicts();
  }
  private dateOnly(value: string): string { return value.split(/[T ]/)[0]; }
  private datePart(value: string): string { return value.slice(0, 10); }
  private timePart(value: string): string { return value.slice(11, 13) + ':00'; }
  private dateTime(date: string, time: string): string { return toApiDateTime(`${date} ${time}`); }
  private durationHours(start: string, end: string): number {
    const hours = Math.max(1, Math.round((
      new Date(toApiDateTime(end)).getTime() - new Date(toApiDateTime(start)).getTime()
    ) / 36e5));
    return Number.isFinite(hours) ? hours : 1;
  }
}
