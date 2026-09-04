import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-trainer-dashboard',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="hero-panel">
      <div>
        <span class="eyebrow">Trainer space</span>
        <h1>Manage your sessions and availability</h1>
        <p>Follow assigned sessions, review learner registrations, and update your profile.</p>
        <a class="btn btn-primary" routerLink="/trainer/my-sessions">View my sessions</a>
      </div>
      <img src="assets/template/img/teacher-explaining.png" alt="Trainer">
    </section>
  `
})
export class TrainerDashboardComponent {}
