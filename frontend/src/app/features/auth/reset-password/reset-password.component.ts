import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="auth-page">
      <div class="auth-hero"><img src="assets/template/img/congrat.svg" alt="Reset password"></div>
      <form class="auth-card" [formGroup]="form" (ngSubmit)="submit()">
        <span class="eyebrow">Account recovery</span>
        <h1>Reset password</h1>
        <p class="muted">Use the email of the account you are recovering. It must match the reset token.</p>
        @if (message()) { <p class="alert success">{{ message() }}</p> }
        @if (error()) { <p class="alert error">{{ error() }}</p> }
        <label>Account email <input type="email" formControlName="email" autocomplete="email"></label>
        <label>Reset token <input formControlName="token"></label>
        <label>New password <input type="password" formControlName="newPassword" autocomplete="new-password"></label>
        <button class="btn btn-primary" [disabled]="form.invalid || loading()">
          {{ loading() ? 'Resetting...' : 'Reset password' }}
        </button>
        <p class="muted"><a routerLink="/login">Back to login</a></p>
      </form>
    </section>
  `
})
export class ResetPasswordComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  loading = signal(false);
  message = signal('');
  error = signal('');
  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    token: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(6)]]
  });

  constructor(private readonly route: ActivatedRoute, private readonly auth: AuthService) {}

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    const email = this.route.snapshot.queryParamMap.get('email');
    if (token) this.form.patchValue({ token });
    if (email) this.form.patchValue({ email });
  }

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set('');
    const value = this.form.getRawValue();
    this.auth.resetPassword(value.email, value.token, value.newPassword).subscribe({
      next: () => {
        this.message.set('Password reset successful. You can login with the new password.');
        this.loading.set(false);
      },
      error: (errorResponse) => {
        this.error.set(errorResponse?.error?.message || 'Invalid email, token, or request.');
        this.loading.set(false);
      }
    });
  }
}
