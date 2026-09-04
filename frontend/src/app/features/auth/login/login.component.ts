import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="auth-page">
      <div class="auth-hero">
        <img src="assets/template/img/hero.png" alt="Training platform">
      </div>
      <form class="auth-card" [formGroup]="form" (ngSubmit)="submit()">
        <span class="eyebrow">Welcome back</span>
        <h1>Sign in to TrainingPro</h1>
        @if (error()) { <p class="alert error">{{ error() }}</p> }
        <label>Email <input type="email" formControlName="email"></label>
        <label>Password <input type="password" formControlName="password"></label>
        <button class="btn btn-primary" [disabled]="form.invalid || loading()">
          {{ loading() ? 'Signing in...' : 'Login' }}
        </button>
        <p class="muted"><a routerLink="/forgot-password">Forgot password?</a></p>
        <p class="muted">No account? <a routerLink="/register">Create learner account</a></p>
      </form>
    </section>
  `
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  loading = signal(false);
  error = signal('');
  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  constructor(private readonly auth: AuthService) {}

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set('');
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => this.auth.redirectAfterLogin(),
      error: (errorResponse) => {
        this.error.set(errorResponse?.error?.message || 'Invalid email or password.');
        this.loading.set(false);
      }
    });
  }
}
