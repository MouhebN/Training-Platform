import { Component, Input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

export interface NavItem {
  label: string;
  route: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <aside class="sidebar">
      <a routerLink="/" class="logo-area">
        <img src="assets/template2/images/logo-icon.svg" alt="" width="26" height="26">
        <span class="logo-text">TrainingPro</span>
      </a>
      <div class="nav-section">{{ title }}</div>
      @for (item of items; track item.route) {
        <a [routerLink]="item.route" routerLinkActive="active" class="sidebar-link">
          <span class="nav-dot"></span>
          {{ item.label }}
        </a>
      }
    </aside>
  `
})
export class SidebarComponent {
  @Input({ required: true }) title = '';
  @Input({ required: true }) items: NavItem[] = [];
}
