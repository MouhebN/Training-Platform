import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from '../navbar/navbar.component';
import { SidebarComponent, NavItem } from '../sidebar/sidebar.component';

@Component({
  selector: 'app-trainer-layout',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, SidebarComponent],
  template: `
    <div class="app-shell">
      <app-sidebar title="Trainer" [items]="items" />
      <section class="workspace">
        <app-navbar />
        <main class="content"><router-outlet /></main>
      </section>
    </div>
  `
})
export class TrainerLayoutComponent {
  items: NavItem[] = [
    { label: 'Dashboard', route: '/trainer/dashboard' },
    { label: 'My sessions', route: '/trainer/my-sessions' },
    { label: 'Availability', route: '/trainer/availability' },
    { label: 'Profile', route: '/trainer/profile' },
    { label: 'Change password', route: '/trainer/change-password' }
  ];
}
