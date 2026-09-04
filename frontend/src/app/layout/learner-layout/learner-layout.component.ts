import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from '../navbar/navbar.component';
import { SidebarComponent, NavItem } from '../sidebar/sidebar.component';

@Component({
  selector: 'app-learner-layout',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, SidebarComponent],
  template: `
    <div class="app-shell">
      <app-sidebar title="Learner" [items]="items" />
      <section class="workspace">
        <app-navbar />
        <main class="content"><router-outlet /></main>
      </section>
    </div>
  `
})
export class LearnerLayoutComponent {
  items: NavItem[] = [
    { label: 'Dashboard', route: '/learner/dashboard' },
    { label: 'Catalogue', route: '/learner/catalogue' },
    { label: 'My enrollments', route: '/learner/my-enrollments' },
    { label: 'Learning Path', route: '/learner/learning-path' },
    { label: 'Improvement plan', route: '/learner/improvement-plan' },
    { label: 'Profile', route: '/learner/profile' },
    { label: 'Change password', route: '/learner/change-password' }
  ];
}
