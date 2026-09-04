import { Injectable, signal } from '@angular/core';

export type ConfirmTone = 'danger' | 'primary' | 'warning';

export interface ConfirmDialogOptions {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  tone?: ConfirmTone;
}

interface ConfirmDialogState extends ConfirmDialogOptions {
  resolve: (value: boolean) => void;
}

@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  readonly state = signal<ConfirmDialogState | null>(null);

  confirm(options: ConfirmDialogOptions): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      const current = this.state();
      if (current) {
        current.resolve(false);
      }
      this.state.set({
        title: options.title,
        message: options.message,
        confirmLabel: options.confirmLabel ?? 'Confirm',
        cancelLabel: options.cancelLabel ?? 'Cancel',
        tone: options.tone ?? 'danger',
        resolve
      });
    });
  }

  close(result: boolean): void {
    const current = this.state();
    if (!current) return;
    this.state.set(null);
    current.resolve(result);
  }
}
