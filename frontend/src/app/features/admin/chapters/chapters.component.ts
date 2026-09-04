import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Chapter, Formation } from '../../../core/models/catalogue.model';
import { ChapterService } from '../../../core/services/chapter.service';
import { ConfirmDialogService } from '../../../core/services/confirm-dialog.service';
import { FormationService } from '../../../core/services/formation.service';

@Component({
  selector: 'app-admin-chapters',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <section class="page-header"><h1>Chapters</h1><p>Manage content by formation.</p></section>
    <div class="panel">
      <label>Formation
        <select [value]="selectedFormation()" (change)="selectFormation($any($event.target).value)">
          <option value="0">Choose formation</option>
          @for (formation of formations(); track formation.id) { <option [value]="formation.id">{{ formation.title }}</option> }
        </select>
      </label>
    </div>
    @if (selectedFormation()) {
      <form class="panel form-grid" [formGroup]="form" (ngSubmit)="create()">
        <input placeholder="Chapter title" formControlName="title">
        <input type="number" placeholder="Order" formControlName="orderIndex">
        <textarea placeholder="Content" formControlName="content"></textarea>
        <button class="btn btn-primary" [disabled]="form.invalid">Add chapter</button>
      </form>
      <div class="panel table-wrap">
        <table><tbody>
          @for (chapter of chapters(); track chapter.id) {
            <tr><td>{{ chapter.orderIndex }}</td><td>{{ chapter.title }}</td><td><button class="btn btn-danger" (click)="remove(chapter)">Delete</button></td></tr>
          }
        </tbody></table>
      </div>
    }
  `
})
export class ChaptersComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  formations = signal<Formation[]>([]);
  chapters = signal<Chapter[]>([]);
  selectedFormation = signal(0);
  form = this.fb.nonNullable.group({ title: ['', Validators.required], content: [''], orderIndex: [1, [Validators.required, Validators.min(1)]] });

  constructor(
    private readonly formationsService: FormationService,
    private readonly chaptersService: ChapterService,
    private readonly confirmDialog: ConfirmDialogService
  ) {}

  ngOnInit(): void { this.formationsService.list({ size: 100 }).subscribe((p) => this.formations.set(p.content)); }
  selectFormation(id: string): void { this.selectedFormation.set(Number(id)); this.load(); }
  load(): void { if (this.selectedFormation()) this.chaptersService.byFormation(this.selectedFormation()).subscribe((d) => this.chapters.set(d)); }
  create(): void { this.chaptersService.create(this.selectedFormation(), this.form.getRawValue()).subscribe(() => { this.form.reset({ title: '', content: '', orderIndex: 1 }); this.load(); }); }
  async remove(chapter: Chapter): Promise<void> {
    const ok = await this.confirmDialog.confirm({
      title: 'Delete chapter?',
      message: `Delete “${chapter.title}”?`,
      confirmLabel: 'Delete',
      tone: 'danger'
    });
    if (!ok) return;
    this.chaptersService.delete(chapter.id).subscribe(() => this.load());
  }
}
