import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <section class="page-header">
      <h1>Change password</h1>
      <p>Update the password for your authenticated account.</p>
    </section>
    <section class="panel">
      @if (message()) { <p class="alert success">{{ message() }}</p> }
      @if (error()) { <p class="alert error">{{ error() }}</p> }
      <form class="form-row" [formGroup]="form" (ngSubmit)="submit()">
        <input type="password" placeholder="Old password" formControlName="oldPassword">
        <input type="password" placeholder="New password" formControlName="newPassword">
        <button class="btn btn-primary" [disabled]="form.invalid">Update password</button>
      </form>
    </section>
  `
})
export class ChangePasswordComponent {
  private readonly fb = inject(FormBuilder);
  message = signal('');
  error = signal('');
  form = this.fb.nonNullable.group({
    oldPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(6)]]
  });

  constructor(private readonly auth: AuthService) {}

  submit(): void {
    const value = this.form.getRawValue();
    this.auth.changePassword(value.oldPassword, value.newPassword).subscribe({
      next: () => { this.message.set('Password changed.'); this.error.set(''); this.form.reset(); },
      error: () => { this.error.set('Old password is incorrect.'); this.message.set(''); }
    });
  }
}
