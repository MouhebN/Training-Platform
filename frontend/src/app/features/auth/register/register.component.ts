import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="auth-page">
      <div class="auth-hero">
        <img src="assets/template/img/girl-with-books.png" alt="Learner">
      </div>
      <form class="auth-card" [formGroup]="form" (ngSubmit)="submit()">
        <span class="eyebrow">Learner account</span>
        <h1>Create your profile</h1>
        @if (error()) { <p class="alert error">{{ error() }}</p> }
        <div class="grid two">
          <label>First name <input formControlName="firstName"></label>
          <label>Last name <input formControlName="lastName"></label>
        </div>
        <label>Email <input type="email" formControlName="email"></label>
        <label>Password <input type="password" formControlName="password"></label>
        <button class="btn btn-primary" [disabled]="form.invalid || loading()">
          {{ loading() ? 'Creating...' : 'Register' }}
        </button>
        <p class="muted">Already registered? <a routerLink="/login">Login</a></p>
      </form>
    </section>
  `
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  loading = signal(false);
  error = signal('');
  form = this.fb.nonNullable.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  constructor(private readonly auth: AuthService) {}

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set('');
    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => this.auth.redirectAfterLogin(),
      error: () => {
        this.error.set('Could not create account. Check the email and try again.');
        this.loading.set(false);
      }
    });
  }
}
