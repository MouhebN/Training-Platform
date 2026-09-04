import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LearnerProfileScore, Skill } from '../../../core/models/profile.model';
import { LearnerService } from '../../../core/services/learner.service';
import { SkillService } from '../../../core/services/skill.service';

@Component({
  selector: 'app-learner-profile',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <section class="page-header"><h1>My profile</h1><p>Keep your learning profile up to date.</p></section>
    @if (message()) { <p class="alert success">{{ message() }}</p> }
    @if (error()) { <p class="alert error">{{ error() }}</p> }
    @if (score(); as s) {
      <section class="panel profile-intelligence">
        <div>
          <span class="eyebrow">Profile completion</span>
          <h2>{{ s.score }}%</h2>
        </div>
        <div class="progress-track"><span [style.width.%]="s.score"></span></div>
        <p>{{ s.message }}</p>
        @if (s.missingFields.length) {
          <p class="muted">Missing: {{ s.missingFields.join(', ') }}</p>
        }
      </section>
    }
    <form class="panel form-grid" [formGroup]="form" (ngSubmit)="save()">
      <label>Email
        <input type="email" placeholder="you@example.com" formControlName="email">
      </label>
      <label>Phone
        <input placeholder="Phone" formControlName="phone">
      </label>
      <label>Level
        <select formControlName="currentLevel">
          <option>BEGINNER</option>
          <option>INTERMEDIATE</option>
          <option>ADVANCED</option>
        </select>
      </label>
      <div class="skill-picker">
        <span>Skills</span>
        <div class="skill-options">
          @for (skill of skills(); track skill.id) {
            <label class="skill-chip">
              <input type="checkbox" [checked]="isSkillSelected(skill.id)" (change)="toggleSkill(skill.id)">
              {{ skill.name }}
            </label>
          }
        </div>
      </div>
      <label class="span-full">Bio
        <textarea placeholder="Bio" formControlName="bio"></textarea>
      </label>
      <label class="span-full">Learning goals
        <textarea placeholder="Learning goals" formControlName="learningGoals"></textarea>
      </label>
      <button class="btn btn-primary" [disabled]="form.invalid">Save profile</button>
    </form>
  `
})
export class LearnerProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  message = signal('');
  error = signal('');
  score = signal<LearnerProfileScore | null>(null);
  skills = signal<Skill[]>([]);
  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    bio: [''],
    currentLevel: ['BEGINNER'],
    learningGoals: [''],
    skillIds: [[] as number[]]
  });
  constructor(private readonly learnerService: LearnerService, private readonly skillService: SkillService) {}
  ngOnInit(): void {
    this.skillService.list().subscribe((skills) => this.skills.set(skills));
    this.learnerService.me().subscribe((p) => this.form.patchValue({
      email: p.user?.email ?? '',
      phone: p.phone ?? '',
      bio: p.bio ?? '',
      currentLevel: p.currentLevel,
      learningGoals: p.learningGoals ?? '',
      skillIds: p.skills?.map((skill) => skill.id) ?? []
    }));
    this.loadScore();
  }
  save(): void {
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    this.learnerService.updateMe({
      ...value,
      skillIds: value.skillIds.map((id) => Number(id))
    }).subscribe({
      next: () => {
        this.message.set('Profile updated.');
        this.error.set('');
        this.loadScore();
      },
      error: () => this.error.set('Could not update profile. Check the email is valid and not already used.')
    });
  }
  private loadScore(): void {
    this.learnerService.profileScore().subscribe((score) => this.score.set(score));
  }
  isSkillSelected(skillId: number): boolean {
    return this.form.controls.skillIds.value.includes(skillId);
  }
  toggleSkill(skillId: number): void {
    const current = this.form.controls.skillIds.value;
    const next = current.includes(skillId)
      ? current.filter((id) => id !== skillId)
      : [...current, skillId];
    this.form.controls.skillIds.setValue(next);
  }
}
