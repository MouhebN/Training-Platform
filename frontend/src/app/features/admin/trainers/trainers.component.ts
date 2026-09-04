import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Skill, Trainer } from '../../../core/models/profile.model';
import { SkillService } from '../../../core/services/skill.service';
import { ConfirmDialogService } from '../../../core/services/confirm-dialog.service';
import { TrainerService } from '../../../core/services/trainer.service';

@Component({
  selector: 'app-admin-trainers',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <section class="page-header">
      <h1>Trainers</h1>
      <p>Create trainer accounts. Each trainer uploads their own CV from their profile.</p>
    </section>
    @if (error()) { <p class="alert error">{{ error() }}</p> }
    @if (message()) { <p class="alert success">{{ message() }}</p> }

    <form class="panel form-grid" [formGroup]="form" (ngSubmit)="create()">
      <section class="span-full">
        <h2>Account</h2>
        <p class="muted">Login details the trainer will use to sign in.</p>
      </section>
      <label>First name
        <input placeholder="Sami" formControlName="firstName">
      </label>
      <label>Last name
        <input placeholder="Ben Ali" formControlName="lastName">
      </label>
      <label>Email
        <input type="email" placeholder="trainer@example.com" formControlName="email">
      </label>
      <label>Password
        <input type="password" placeholder="At least 6 characters" formControlName="password">
      </label>

      <section class="span-full nested-form">
        <h2>Profile</h2>
        <p class="muted">Optional details you can fill now. The trainer can complete the rest later, including their CV.</p>
      </section>
      <label>Phone
        <input placeholder="Optional phone" formControlName="phone">
      </label>
      <label>Years of experience
        <input type="number" min="0" step="1" formControlName="yearsOfExperience">
      </label>
      <div class="skill-picker span-full">
        <span>Expertise</span>
        <div class="skill-options">
          @for (skill of skills(); track skill.id) {
            <label class="skill-chip">
              <input type="checkbox" [checked]="isSkillSelected(skill.id)" (change)="toggleSkill(skill.id)">
              {{ skill.name }}
            </label>
          } @empty {
            <span class="muted">No skills yet. Add them in Skills first.</span>
          }
        </div>
      </div>
      <label class="span-full">Bio
        <textarea placeholder="Optional bio" formControlName="bio"></textarea>
      </label>
      <p class="span-full muted">CVs are uploaded by the trainer after they sign in. You can view them in the list below.</p>
      <button class="btn btn-primary" [disabled]="form.invalid">Create trainer</button>
    </form>

    <div class="panel table-wrap">
      <h2>All trainers</h2>
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Email</th>
            <th>Expertise</th>
            <th>Experience</th>
            <th>CV</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          @for (trainer of trainers(); track trainer.id) {
            <tr>
              <td>{{ trainer.user.firstName }} {{ trainer.user.lastName }}</td>
              <td>{{ trainer.user.email }}</td>
              <td>{{ expertiseNames(trainer) }}</td>
              <td>{{ trainer.yearsOfExperience }} years</td>
              <td>
                @if (trainer.cvUrl) {
                  <span class="badge badge-success">Uploaded</span>
                } @else {
                  <span class="badge badge-warning">Waiting</span>
                }
              </td>
              <td class="actions">
                @if (trainer.cvUrl) {
                  <button class="btn btn-light" type="button" (click)="viewCv(trainer)">View CV</button>
                }
                <button class="btn btn-danger" type="button" (click)="remove(trainer)">Delete</button>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="6" class="muted">No trainers yet.</td></tr>
          }
        </tbody>
      </table>
    </div>
  `
})
export class TrainersComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  trainers = signal<Trainer[]>([]);
  skills = signal<Skill[]>([]);
  error = signal('');
  message = signal('');
  form = this.fb.nonNullable.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    phone: [''],
    bio: [''],
    yearsOfExperience: [0, [Validators.required, Validators.min(0)]],
    expertiseSkillIds: [[] as number[]]
  });

  constructor(
    private readonly trainerService: TrainerService,
    private readonly skillService: SkillService,
    private readonly confirmDialog: ConfirmDialogService
  ) {}
  ngOnInit(): void { this.load(); this.skillService.list().subscribe((d) => this.skills.set(d)); }
  load(): void { this.trainerService.list().subscribe((d) => this.trainers.set(d)); }
  create(): void {
    this.trainerService.create(this.form.getRawValue()).subscribe({
      next: () => {
        this.form.reset({ yearsOfExperience: 0, expertiseSkillIds: [], firstName: '', lastName: '', email: '', password: '', phone: '', bio: '' });
        this.message.set('Trainer created. They can upload their CV from their profile.');
        this.error.set('');
        this.load();
      },
      error: () => this.error.set('Could not create trainer. Check the email is not already used.')
    });
  }
  async remove(trainer: Trainer): Promise<void> {
    const ok = await this.confirmDialog.confirm({
      title: 'Delete trainer?',
      message: `Delete trainer ${trainer.user.email}?`,
      confirmLabel: 'Delete',
      tone: 'danger'
    });
    if (!ok) return;
    this.trainerService.delete(trainer.id).subscribe(() => this.load());
  }
  viewCv(trainer: Trainer): void {
    this.trainerService.downloadCv(trainer.id).subscribe({
      next: (blob) => this.openBlob(blob),
      error: () => this.error.set('Could not open this CV.')
    });
  }
  isSkillSelected(skillId: number): boolean {
    return this.form.controls.expertiseSkillIds.value.includes(skillId);
  }
  toggleSkill(skillId: number): void {
    const current = this.form.controls.expertiseSkillIds.value;
    this.form.controls.expertiseSkillIds.setValue(
      current.includes(skillId) ? current.filter((id) => id !== skillId) : [...current, skillId]
    );
  }
  expertiseNames(trainer: Trainer): string {
    return trainer.expertise?.length ? trainer.expertise.map((skill) => skill.name).join(', ') : 'None';
  }

  private openBlob(blob: Blob): void {
    if (blob.type.includes('json')) {
      this.error.set('Could not open this CV.');
      return;
    }
    window.open(URL.createObjectURL(blob), '_blank', 'noopener');
  }
}
