import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="auth-page">
      <div class="auth-hero"><img src="assets/template/img/true-false.png" alt="Password reset"></div>
      <form class="auth-card" [formGroup]="form" (ngSubmit)="submit()">
        <span class="eyebrow">Account recovery</span>
        <h1>Forgot password</h1>
        <p class="muted">Enter the email of your account. A reset token will be sent only if that account exists.</p>
        @if (message()) { <p class="alert success">{{ message() }}</p> }
        @if (error()) { <p class="alert error">{{ error() }}</p> }
        <label>Email <input type="email" formControlName="email" autocomplete="email"></label>
        <button class="btn btn-primary" [disabled]="form.invalid || loading()">
          {{ loading() ? 'Sending...' : 'Send reset token' }}
        </button>
        <p class="muted">After receiving the token, open <a routerLink="/reset-password">reset password</a>.</p>
        <p class="muted"><a routerLink="/login">Back to login</a></p>
      </form>
    </section>
  `
})
export class ForgotPasswordComponent {
  private readonly fb = inject(FormBuilder);
  loading = signal(false);
  message = signal('');
  error = signal('');
  form = this.fb.nonNullable.group({ email: ['', [Validators.required, Validators.email]] });

  constructor(private readonly auth: AuthService) {}

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set('');
    this.auth.forgotPassword(this.form.getRawValue().email).subscribe({
      next: (response) => {
        this.message.set(response.message);
        this.loading.set(false);
      },
      error: (errorResponse) => {
        this.error.set(errorResponse?.error?.message || 'Could not send the reset token.');
        this.loading.set(false);
      }
    });
  }
}
