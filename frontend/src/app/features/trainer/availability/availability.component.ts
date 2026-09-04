import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TrainerAvailability } from '../../../core/models/profile.model';
import { ConfirmDialogService } from '../../../core/services/confirm-dialog.service';
import { TrainerService } from '../../../core/services/trainer.service';
import { formatTime24 } from '../../../core/utils/date-time.util';

interface DayCard {
  dayOfWeek: string;
  label: string;
  short: string;
  slot: TrainerAvailability | null;
}

@Component({
  selector: 'app-trainer-availability',
  standalone: true,
  imports: [ReactiveFormsModule],
  styles: [`
    .week-grid {
      display: grid;
      grid-template-columns: repeat(7, minmax(0, 1fr));
      gap: 10px;
      margin-bottom: 18px;
    }
    .day-card {
      border: 1px solid var(--line);
      border-radius: 14px;
      background: #fff;
      padding: 14px 12px;
      text-align: left;
      cursor: pointer;
      min-height: 128px;
      display: grid;
      gap: 8px;
      align-content: start;
      transition: border-color .15s ease, box-shadow .15s ease, transform .15s ease;
    }
    .day-card:hover {
      border-color: rgba(230, 98, 57, .45);
      box-shadow: 0 10px 24px rgba(23, 23, 23, .06);
      transform: translateY(-1px);
    }
    .day-card.active {
      border-color: var(--primary);
      box-shadow: 0 0 0 3px var(--primary-soft);
    }
    .day-card.set {
      background: linear-gradient(180deg, var(--primary-soft), #fff 55%);
    }
    .day-card .short {
      font-size: 12px;
      font-weight: 700;
      letter-spacing: .06em;
      text-transform: uppercase;
      color: var(--muted);
    }
    .day-card .label {
      font-size: 15px;
      font-weight: 600;
      margin: 0;
    }
    .day-card .hours {
      font-size: 13px;
      color: var(--ink, #171717);
      font-weight: 600;
    }
    .day-card .empty {
      font-size: 12px;
      color: var(--muted);
    }
    .editor {
      display: grid;
      gap: 14px;
    }
    .editor-head {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      align-items: baseline;
      flex-wrap: wrap;
    }
    .editor-head h2 { margin: 0; font-size: 16px; }
    .time-row {
      display: grid;
      grid-template-columns: 1fr 1fr auto;
      gap: 12px;
      align-items: end;
    }
    .actions {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
    }
    @media (max-width: 960px) {
      .week-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
      .time-row { grid-template-columns: 1fr; }
    }
  `],
  template: `
    <section class="page-header">
      <h1>Availability</h1>
      <p>One time window per weekday. Click a day to set or update it anytime.</p>
    </section>
    @if (error()) { <p class="alert error">{{ error() }}</p> }
    @if (message()) { <p class="alert success">{{ message() }}</p> }

    <div class="week-grid">
      @for (day of days(); track day.dayOfWeek) {
        <button
          type="button"
          class="day-card"
          [class.set]="!!day.slot"
          [class.active]="selectedDay() === day.dayOfWeek"
          (click)="selectDay(day)">
          <span class="short">{{ day.short }}</span>
          <p class="label">{{ day.label }}</p>
          @if (day.slot) {
            <span class="hours">{{ formatTime(day.slot.startTime) }} – {{ formatTime(day.slot.endTime) }}</span>
          } @else {
            <span class="empty">Off</span>
          }
        </button>
      }
    </div>

    @if (selectedDay()) {
      <form class="panel editor" [formGroup]="form" (ngSubmit)="save()">
        <div class="editor-head">
          <div>
            <h2>{{ selectedLabel() }}</h2>
            <p class="muted">
              {{ editingSlot() ? 'Update this day’s hours or clear it.' : 'Set your available hours for this day.' }}
            </p>
          </div>
        </div>
        <div class="time-row">
          <label>Start (24-hour)
            <select formControlName="startTime">
              @for (time of timeOptions; track time) { <option [value]="time">{{ time }}</option> }
            </select>
          </label>
          <label>End (24-hour)
            <select formControlName="endTime">
              @for (time of timeOptions; track time) { <option [value]="time">{{ time }}</option> }
            </select>
          </label>
          <div class="actions">
            <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving()">
              {{ saving() ? 'Saving...' : (editingSlot() ? 'Update' : 'Save') }}
            </button>
            @if (editingSlot()) {
              <button class="btn btn-danger" type="button" (click)="clear()" [disabled]="saving()">Clear day</button>
            }
          </div>
        </div>
      </form>
    }
  `
})
export class AvailabilityComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dayMeta: { dayOfWeek: string; label: string; short: string }[] = [
    { dayOfWeek: 'MONDAY', label: 'Monday', short: 'Mon' },
    { dayOfWeek: 'TUESDAY', label: 'Tuesday', short: 'Tue' },
    { dayOfWeek: 'WEDNESDAY', label: 'Wednesday', short: 'Wed' },
    { dayOfWeek: 'THURSDAY', label: 'Thursday', short: 'Thu' },
    { dayOfWeek: 'FRIDAY', label: 'Friday', short: 'Fri' },
    { dayOfWeek: 'SATURDAY', label: 'Saturday', short: 'Sat' },
    { dayOfWeek: 'SUNDAY', label: 'Sunday', short: 'Sun' }
  ];

  readonly timeOptions = Array.from({ length: 24 }, (_, hour) => `${hour.toString().padStart(2, '0')}:00`);
  trainerId = signal(0);
  availability = signal<TrainerAvailability[]>([]);
  selectedDay = signal<string | null>(null);
  error = signal('');
  message = signal('');
  saving = signal(false);

  form = this.fb.nonNullable.group({
    dayOfWeek: ['MONDAY', Validators.required],
    startTime: ['09:00', [Validators.required, Validators.pattern(/^([01]\d|2[0-3]):[0-5]\d$/)]],
    endTime: ['17:00', [Validators.required, Validators.pattern(/^([01]\d|2[0-3]):[0-5]\d$/)]]
  });

  days = computed<DayCard[]>(() => {
    const byDay = new Map<string, TrainerAvailability>();
    for (const slot of this.availability()) {
      if (!byDay.has(slot.dayOfWeek)) {
        byDay.set(slot.dayOfWeek, slot);
      }
    }
    return this.dayMeta.map((meta) => ({
      ...meta,
      slot: byDay.get(meta.dayOfWeek) ?? null
    }));
  });

  editingSlot = computed(() => this.days().find((day) => day.dayOfWeek === this.selectedDay())?.slot ?? null);
  selectedLabel = computed(() => this.days().find((day) => day.dayOfWeek === this.selectedDay())?.label ?? '');

  constructor(
    private readonly trainerService: TrainerService,
    private readonly confirmDialog: ConfirmDialogService
  ) {}

  ngOnInit(): void {
    this.trainerService.me().subscribe((trainer) => {
      this.trainerId.set(trainer.id);
      this.load();
    });
  }

  load(): void {
    if (!this.trainerId()) return;
    this.trainerService.availability(this.trainerId()).subscribe((slots) => this.availability.set(slots));
  }

  selectDay(day: DayCard): void {
    this.error.set('');
    this.message.set('');
    this.selectedDay.set(day.dayOfWeek);
    this.form.patchValue({
      dayOfWeek: day.dayOfWeek,
      startTime: this.toHourOption(day.slot?.startTime) || '09:00',
      endTime: this.toHourOption(day.slot?.endTime) || '17:00'
    });
  }

  save(): void {
    if (this.form.invalid || !this.selectedDay()) return;
    this.saving.set(true);
    this.error.set('');
    this.message.set('');
    const payload = this.form.getRawValue();
    const wasSet = !!this.editingSlot();

    // Backend upserts by day (one window per weekday) and cleans duplicates.
    this.trainerService.createAvailability(payload).subscribe({
      next: () => {
        this.message.set(wasSet ? `${this.selectedLabel()} updated.` : `${this.selectedLabel()} saved.`);
        this.saving.set(false);
        this.load();
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Could not save availability. End time must be after start time.');
        this.saving.set(false);
      }
    });
  }

  async clear(): Promise<void> {
    const slot = this.editingSlot();
    if (!slot) return;
    const ok = await this.confirmDialog.confirm({
      title: 'Clear this day?',
      message: `Remove availability for ${this.selectedLabel()}?`,
      confirmLabel: 'Clear day',
      tone: 'danger'
    });
    if (!ok) return;
    this.saving.set(true);
    this.trainerService.deleteAvailability(slot.id).subscribe({
      next: () => {
        this.message.set(`${this.selectedLabel()} cleared.`);
        this.saving.set(false);
        this.load();
      },
      error: () => {
        this.error.set('Could not clear this day.');
        this.saving.set(false);
      }
    });
  }

  formatTime(value: string): string {
    return formatTime24(value);
  }

  private toHourOption(value?: string): string {
    if (!value) return '';
    return value.length >= 5 ? value.slice(0, 5) : value;
  }
}
