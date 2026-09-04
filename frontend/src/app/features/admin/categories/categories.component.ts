import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Category } from '../../../core/models/catalogue.model';
import { CategoryService } from '../../../core/services/category.service';
import { ConfirmDialogService } from '../../../core/services/confirm-dialog.service';

@Component({
  selector: 'app-admin-categories',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <section class="page-header"><h1>Categories</h1><p>Manage catalogue groups.</p></section>
    @if (message()) { <p class="alert success">{{ message() }}</p> }
    @if (error()) { <p class="alert error">{{ error() }}</p> }
    <form class="panel form-row" [formGroup]="form" (ngSubmit)="save()">
      <label>Name
        <input placeholder="IT" formControlName="name">
      </label>
      <label>Description
        <input placeholder="Optional description" formControlName="description">
      </label>
      <button class="btn btn-primary" [disabled]="form.invalid">{{ editingId() ? 'Update' : 'Create' }}</button>
      @if (editingId()) { <button class="btn btn-light" type="button" (click)="reset()">Cancel</button> }
    </form>
    <div class="panel table-wrap">
      <table>
        <thead><tr><th>Name</th><th>Description</th><th></th></tr></thead>
        <tbody>
          @for (category of categories(); track category.id) {
            <tr>
              <td>{{ category.name }}</td>
              <td>{{ category.description }}</td>
              <td class="actions">
                <button class="btn btn-light" (click)="edit(category)">Edit</button>
                <button class="btn btn-danger" (click)="remove(category)">Delete</button>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `
})
export class CategoriesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  categories = signal<Category[]>([]);
  editingId = signal<number | null>(null);
  message = signal('');
  error = signal('');
  form = this.fb.nonNullable.group({ name: ['', Validators.required], description: [''] });

  constructor(
    private readonly service: CategoryService,
    private readonly confirmDialog: ConfirmDialogService
  ) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.service.list().subscribe({ next: (data) => this.categories.set(data), error: () => this.error.set('Failed to load categories.') });
  }

  save(): void {
    const payload = this.form.getRawValue();
    const request = this.editingId()
      ? this.service.update(this.editingId()!, payload)
      : this.service.create(payload);
    request.subscribe({ next: () => { this.message.set('Saved.'); this.reset(); this.load(); }, error: () => this.error.set('Save failed.') });
  }

  edit(category: Category): void {
    this.editingId.set(category.id);
    this.form.patchValue({ name: category.name, description: category.description ?? '' });
  }

  async remove(category: Category): Promise<void> {
    const ok = await this.confirmDialog.confirm({
      title: 'Delete category?',
      message: `Delete category “${category.name}”?`,
      confirmLabel: 'Delete',
      tone: 'danger'
    });
    if (!ok) return;
    this.service.delete(category.id).subscribe({ next: () => this.load(), error: () => this.error.set('Delete failed.') });
  }

  reset(): void {
    this.editingId.set(null);
    this.form.reset();
  }
}
