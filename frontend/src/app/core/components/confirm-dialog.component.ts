import { Component, effect, HostListener } from '@angular/core';
import { ConfirmDialogService } from '../services/confirm-dialog.service';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  styles: [`
    .overlay {
      position: fixed;
      inset: 0;
      z-index: 1000;
      background: rgba(23, 23, 23, .42);
      display: grid;
      place-items: center;
      padding: 20px;
      animation: fade .14s ease;
    }
    .dialog {
      width: min(440px, 100%);
      background: #fff;
      border: 1px solid var(--line);
      border-radius: 14px;
      box-shadow: 0 24px 60px rgba(23, 23, 23, .18);
      padding: 22px 22px 18px;
      display: grid;
      gap: 14px;
      animation: rise .16s ease;
    }
    .dialog h2 {
      margin: 0;
      font-size: 18px;
      line-height: 1.3;
      color: var(--ink);
    }
    .dialog p {
      margin: 0;
      color: var(--muted);
      line-height: 1.5;
      font-size: 14px;
      white-space: pre-wrap;
    }
    .actions {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      margin-top: 4px;
    }
    .tone-danger { border-top: 3px solid var(--danger); }
    .tone-primary { border-top: 3px solid var(--primary); }
    .tone-warning { border-top: 3px solid var(--warning); }
    @keyframes fade {
      from { opacity: 0; }
      to { opacity: 1; }
    }
    @keyframes rise {
      from { opacity: 0; transform: translateY(8px) scale(.98); }
      to { opacity: 1; transform: translateY(0) scale(1); }
    }
  `],
  template: `
    @if (dialog.state(); as current) {
      <div class="overlay" (click)="dialog.close(false)" role="presentation">
        <div
          class="dialog"
          [class.tone-danger]="current.tone === 'danger'"
          [class.tone-primary]="current.tone === 'primary'"
          [class.tone-warning]="current.tone === 'warning'"
          role="alertdialog"
          aria-modal="true"
          [attr.aria-labelledby]="'confirm-title'"
          (click)="$event.stopPropagation()">
          <h2 id="confirm-title">{{ current.title }}</h2>
          <p>{{ current.message }}</p>
          <div class="actions">
            <button type="button" class="btn btn-light" (click)="dialog.close(false)">
              {{ current.cancelLabel }}
            </button>
            <button
              type="button"
              class="btn"
              [class.btn-danger]="current.tone === 'danger'"
              [class.btn-primary]="current.tone !== 'danger'"
              (click)="dialog.close(true)">
              {{ current.confirmLabel }}
            </button>
          </div>
        </div>
      </div>
    }
  `
})
export class ConfirmDialogComponent {
  constructor(readonly dialog: ConfirmDialogService) {
    effect(() => {
      const open = !!dialog.state();
      document.body.style.overflow = open ? 'hidden' : '';
    });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.dialog.state()) {
      this.dialog.close(false);
    }
  }
}
