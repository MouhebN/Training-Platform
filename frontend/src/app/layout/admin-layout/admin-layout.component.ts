import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from '../navbar/navbar.component';
import { SidebarComponent, NavItem } from '../sidebar/sidebar.component';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, SidebarComponent],
  template: `
    <div class="app-shell">
      <app-sidebar title="Admin" [items]="items" />
      <section class="workspace">
        <app-navbar />
        <main class="content"><router-outlet /></main>
      </section>
    </div>
  `
})
export class AdminLayoutComponent {
  items: NavItem[] = [
    { label: 'Dashboard', route: '/admin/dashboard' },
    { label: 'Categories', route: '/admin/categories' },
    { label: 'Formations', route: '/admin/formations' },
    { label: 'Trainers', route: '/admin/trainers' },
    { label: 'Sessions', route: '/admin/sessions' },
    { label: 'Enrollments', route: '/admin/enrollments' },
    { label: 'Trainer Workload', route: '/admin/trainer-workload' },
    { label: 'Users', route: '/admin/users' },
    { label: 'MLA Center', route: '/admin/mla' },
    { label: 'Change password', route: '/admin/change-password' }
  ];
}
