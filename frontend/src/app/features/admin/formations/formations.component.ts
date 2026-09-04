import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { Category, Chapter, Formation } from '../../../core/models/catalogue.model';
import { Skill } from '../../../core/models/profile.model';
import { CategoryService } from '../../../core/services/category.service';
import { ChapterService } from '../../../core/services/chapter.service';
import { ConfirmDialogService } from '../../../core/services/confirm-dialog.service';
import { FormationService } from '../../../core/services/formation.service';
import { SkillService } from '../../../core/services/skill.service';

interface DraftChapter {
  title: string;
  content: string;
  orderIndex: number;
}

@Component({
  selector: 'app-admin-formations',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <section class="page-header">
      <h1>Formations</h1>
      <p>Create a training offer and its chapters together.</p>
    </section>
    @if (error()) { <p class="alert error">{{ error() }}</p> }
    @if (message()) { <p class="alert success">{{ message() }}</p> }

    <form class="panel form-grid" [formGroup]="form" (ngSubmit)="create()">
      <label>Title
        <input placeholder="Spring Boot Fundamentals" formControlName="title">
      </label>
      <label>Category
        <select formControlName="categoryId">
          <option [ngValue]="0">Choose category</option>
          @for (category of categories(); track category.id) {
            <option [ngValue]="category.id">{{ category.name }}</option>
          }
        </select>
      </label>
      <label>Level
        <select formControlName="level">
          <option>BEGINNER</option>
          <option>INTERMEDIATE</option>
          <option>ADVANCED</option>
        </select>
      </label>
      <label>Duration (hours)
        <input type="number" min="1" step="1" formControlName="durationHours">
      </label>
      <label>Number of sessions (séances)
        <input type="number" min="1" step="1" formControlName="sessionCount">
      </label>
      <p class="muted span-full">Set how many live sessions this formation includes. Learner progress is split evenly (e.g. 4 sessions = 25% each).</p>
      <label>Price
        <input type="number" min="0" step="1" formControlName="price">
      </label>
      <div class="skill-picker">
        <span>Required skills</span>
        <div class="skill-options">
          @for (skill of skills(); track skill.id) {
            <label class="skill-chip">
              <input type="checkbox" [checked]="isSkillSelected(skill.id)" (change)="toggleSkill(skill.id)">
              {{ skill.name }}
            </label>
          }
        </div>
      </div>
      <label class="span-full">Description
        <textarea placeholder="Optional description" formControlName="description"></textarea>
      </label>

      <section class="span-full nested-form">
        <h2>Chapters</h2>
        <p class="muted">Add chapters now. They are saved with the formation.</p>
        <div class="form-grid">
          <label>Chapter title
            <input placeholder="Introduction" [formControl]="chapterForm.controls.title">
          </label>
          <label class="span-full">Content
            <textarea placeholder="Chapter content" [formControl]="chapterForm.controls.content"></textarea>
          </label>
        </div>
        <button class="btn btn-light" type="button" [disabled]="!chapterForm.controls.title.value.trim()" (click)="addDraftChapter()">
          Add chapter
        </button>
        @for (chapter of draftChapters(); track chapter.orderIndex) {
          <div class="list-row">
            <strong>{{ chapter.orderIndex }}. {{ chapter.title }}</strong>
            <span class="muted">{{ chapter.content }}</span>
            <button class="btn btn-danger" type="button" (click)="removeDraftChapter(chapter.orderIndex)">Remove</button>
          </div>
        } @empty {
          <p class="muted">No chapters added yet.</p>
        }
      </section>

      <button class="btn btn-primary" [disabled]="form.invalid">Create formation</button>
    </form>

    <div class="card-grid">
      @for (formation of formations(); track formation.id) {
        <article class="item-card">
          <h3>{{ formation.title }}</h3>
          <p>{{ formation.description }}</p>
          <div class="meta">{{ formation.category.name }} · {{ formation.level }} · {{ formation.durationHours }}h · {{ formation.sessionCount }} sessions · {{ formation.price ?? 0 }}</div>
          <div class="meta">Required: {{ skillNames(formation) }}</div>
          <div class="actions">
            <button class="btn btn-light" type="button" (click)="toggleChapters(formation)">
              {{ selectedFormationId() === formation.id ? 'Hide chapters' : 'View chapters' }}
            </button>
            <button class="btn btn-danger" type="button" (click)="remove(formation)">Delete</button>
          </div>
          @if (selectedFormationId() === formation.id) {
            @for (chapter of chapters(); track chapter.id) {
              <div class="list-row">
                <strong>{{ chapter.orderIndex }}. {{ chapter.title }}</strong>
                <span class="muted">{{ chapter.content }}</span>
              </div>
            } @empty {
              <p class="muted">No chapters.</p>
            }
          }
        </article>
      }
    </div>
  `
})
export class FormationsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  categories = signal<Category[]>([]);
  skills = signal<Skill[]>([]);
  formations = signal<Formation[]>([]);
  chapters = signal<Chapter[]>([]);
  draftChapters = signal<DraftChapter[]>([]);
  selectedFormationId = signal(0);
  error = signal('');
  message = signal('');
  form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    description: [''],
    price: [0, [Validators.min(0)]],
    level: ['BEGINNER' as const],
    durationHours: [1, [Validators.required, Validators.min(1)]],
    sessionCount: [1, [Validators.required, Validators.min(1)]],
    active: [true],
    categoryId: [0, [Validators.required, Validators.min(1)]],
    requiredSkillIds: [[] as number[]]
  });
  chapterForm = this.fb.nonNullable.group({
    title: [''],
    content: ['']
  });

  constructor(
    private readonly categoriesService: CategoryService,
    private readonly formationsService: FormationService,
    private readonly skillService: SkillService,
    private readonly chaptersService: ChapterService,
    private readonly confirmDialog: ConfirmDialogService
  ) {}

  ngOnInit(): void {
    this.categoriesService.list().subscribe((d) => this.categories.set(d));
    this.skillService.list().subscribe((d) => this.skills.set(d));
    this.load();
  }

  load(): void {
    this.formationsService.list({ size: 100 }).subscribe((page) => this.formations.set(page.content));
  }

  addDraftChapter(): void {
    const title = this.chapterForm.controls.title.value.trim();
    if (!title) return;
    this.draftChapters.update((chapters) => [
      ...chapters,
      {
        title,
        content: this.chapterForm.controls.content.value.trim(),
        orderIndex: chapters.length + 1
      }
    ]);
    this.chapterForm.reset({ title: '', content: '' });
  }

  removeDraftChapter(orderIndex: number): void {
    this.draftChapters.update((chapters) =>
      chapters
        .filter((chapter) => chapter.orderIndex !== orderIndex)
        .map((chapter, index) => ({ ...chapter, orderIndex: index + 1 }))
    );
  }

  create(): void {
    const drafts = this.draftChapters();
    this.formationsService.create(this.form.getRawValue()).pipe(
      switchMap((formation) => drafts.length
        ? forkJoin(drafts.map((chapter) => this.chaptersService.create(formation.id, chapter)))
        : of([])
      )
    ).subscribe({
      next: () => {
        this.form.reset({ level: 'BEGINNER', durationHours: 1, sessionCount: 1, price: 0, active: true, categoryId: 0, title: '', description: '', requiredSkillIds: [] });
        this.chapterForm.reset({ title: '', content: '' });
        this.draftChapters.set([]);
        this.message.set(drafts.length ? 'Formation and chapters created.' : 'Formation created.');
        this.error.set('');
        this.load();
      },
      error: () => this.error.set('Could not create formation.')
    });
  }

  async remove(formation: Formation): Promise<void> {
    const ok = await this.confirmDialog.confirm({
      title: 'Delete formation?',
      message: `Delete “${formation.title}”?`,
      confirmLabel: 'Delete',
      tone: 'danger'
    });
    if (!ok) return;
    this.formationsService.delete(formation.id).subscribe({
      next: () => {
        if (this.selectedFormationId() === formation.id) {
          this.selectedFormationId.set(0);
          this.chapters.set([]);
        }
        this.load();
      },
      error: () => this.error.set('Delete failed.')
    });
  }

  toggleChapters(formation: Formation): void {
    if (this.selectedFormationId() === formation.id) {
      this.selectedFormationId.set(0);
      this.chapters.set([]);
      return;
    }
    this.selectedFormationId.set(formation.id);
    this.chaptersService.byFormation(formation.id).subscribe((chapters) => this.chapters.set(chapters));
  }

  isSkillSelected(skillId: number): boolean {
    return this.form.controls.requiredSkillIds.value.includes(skillId);
  }

  toggleSkill(skillId: number): void {
    const current = this.form.controls.requiredSkillIds.value;
    this.form.controls.requiredSkillIds.setValue(
      current.includes(skillId) ? current.filter((id) => id !== skillId) : [...current, skillId]
    );
  }

  skillNames(formation: Formation): string {
    return formation.requiredSkills?.length ? formation.requiredSkills.map((skill) => skill.name).join(', ') : 'None';
  }
}
