import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Skill } from '../../../core/models/profile.model';
import { SkillService } from '../../../core/services/skill.service';
import { TrainerService } from '../../../core/services/trainer.service';

@Component({
  selector: 'app-trainer-profile',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <section class="page-header">
      <h1>Trainer profile</h1>
      <p>Update your details and upload your CV. Admins can view it from the trainers list.</p>
    </section>
    @if (message()) { <p class="alert success">{{ message() }}</p> }
    @if (error()) { <p class="alert error">{{ error() }}</p> }

    <form class="panel form-grid" [formGroup]="form" (ngSubmit)="save()">
      <label>Phone
        <input placeholder="Phone" formControlName="phone">
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
          }
        </div>
      </div>
      <label class="span-full">Bio
        <textarea placeholder="Optional bio" formControlName="bio"></textarea>
      </label>
      <button class="btn btn-primary">Save profile</button>
    </form>

    <section class="panel form-grid">
      <div class="span-full">
        <h2>CV</h2>
        <p class="muted">Upload a PDF or Word file, max 5MB. Replacing it overwrites the previous file.</p>
      </div>
      @if (hasCv()) {
        <div class="span-full">
          <span class="badge badge-success">CV uploaded</span>
          <button class="btn btn-light" type="button" (click)="viewCv()">View current CV</button>
        </div>
      } @else {
        <p class="span-full muted">No CV uploaded yet.</p>
      }
      <label class="span-full file-field">Choose file
        <input type="file" accept=".pdf,.doc,.docx,application/pdf" (change)="onCvSelected($event)">
      </label>
      @if (selectedCvName()) {
        <p class="span-full muted">Selected: {{ selectedCvName() }}</p>
      }
      <button class="btn btn-primary" type="button" [disabled]="!selectedCv" (click)="uploadCv()">Upload CV</button>
    </section>
  `
})
export class TrainerProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  message = signal('');
  error = signal('');
  hasCv = signal(false);
  selectedCvName = signal('');
  skills = signal<Skill[]>([]);
  selectedCv: File | null = null;
  form = this.fb.nonNullable.group({
    phone: [''],
    bio: [''],
    yearsOfExperience: [0],
    expertiseSkillIds: [[] as number[]],
    active: [true]
  });

  constructor(private readonly trainerService: TrainerService, private readonly skillService: SkillService) {}

  ngOnInit(): void {
    this.skillService.list().subscribe((skills) => this.skills.set(skills));
    this.trainerService.me().subscribe((profile) => {
      this.hasCv.set(!!profile.cvUrl);
      this.form.patchValue({
        phone: profile.phone ?? '',
        bio: profile.bio ?? '',
        yearsOfExperience: profile.yearsOfExperience,
        expertiseSkillIds: profile.expertise?.map((skill) => skill.id) ?? [],
        active: profile.active
      });
    });
  }

  onCvSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedCv = input.files?.[0] ?? null;
    this.selectedCvName.set(this.selectedCv?.name ?? '');
  }

  save(): void {
    this.trainerService.updateMe(this.form.getRawValue()).subscribe({
      next: () => {
        this.message.set('Profile updated.');
        this.error.set('');
      },
      error: () => this.error.set('Could not update profile.')
    });
  }

  uploadCv(): void {
    if (!this.selectedCv) return;
    this.trainerService.uploadCv(this.selectedCv).subscribe({
      next: () => {
        this.hasCv.set(true);
        this.selectedCv = null;
        this.selectedCvName.set('');
        this.message.set('CV uploaded.');
        this.error.set('');
      },
      error: () => this.error.set('The CV must be a PDF or Word file under 5MB.')
    });
  }

  viewCv(): void {
    this.trainerService.downloadMyCv().subscribe({
      next: (blob) => {
        if (blob.type.includes('json')) {
          this.error.set('Could not open your CV.');
          return;
        }
        window.open(URL.createObjectURL(blob), '_blank', 'noopener');
      },
      error: () => this.error.set('Could not open your CV.')
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
}
