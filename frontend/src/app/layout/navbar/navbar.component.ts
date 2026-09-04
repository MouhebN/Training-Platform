import { Component, HostListener, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { AppNotification, NotificationApiService } from '../../core/services/notification.service';
import { NotificationSoundService } from '../../core/services/notification-sound.service';
import { StompService } from '../../core/services/stomp.service';
import { formatDateTime24 } from '../../core/utils/date-time.util';

@Component({
  selector: 'app-navbar',
  standalone: true,
  template: `
    <header class="topbar">
      <div class="topbar-left">
        <button class="icon-button" type="button" aria-label="Toggle sidebar">☰</button>
        <span class="topbar-title">Professional Training Platform</span>
      </div>
      <div class="topbar-user">
        <div class="notification-menu">
          <button class="notification-button" type="button" aria-label="Notifications" (click)="toggleInbox($event)">
            @if (unread()) { <span class="notification-dot">{{ unread() > 9 ? '9+' : unread() }}</span> }
            @else { ● }
          </button>
          @if (open()) {
            <div class="notification-panel" (click)="$event.stopPropagation()">
              <div class="inline spread">
                <strong>Notifications</strong>
                @if (unread()) {
                  <button class="btn btn-light" type="button" (click)="markAll()">Mark all read</button>
                }
              </div>
              @for (item of items(); track item.id) {
                <button class="notification-item" [class.unread]="!item.read" type="button" (click)="openItem(item)">
                  <strong>{{ item.title }}</strong>
                  <span>{{ item.body }}</span>
                  <small>{{ formatWhen(item.createdAt) }}</small>
                </button>
              } @empty {
                <p class="muted">No notifications yet.</p>
              }
            </div>
          }
        </div>
        <div class="user-pill">
          <span class="avatar">{{ initials() }}</span>
          <div>
            <strong>{{ auth.currentUser()?.firstName }} {{ auth.currentUser()?.lastName }}</strong>
            <small>{{ auth.currentUser()?.role }}</small>
          </div>
        </div>
        <button class="btn btn-light" type="button" (click)="logout()">Logout</button>
      </div>
    </header>
  `
})
export class NavbarComponent implements OnInit {
  items = signal<AppNotification[]>([]);
  unread = signal(0);
  open = signal(false);
  private subscribedUserId?: number;

  constructor(
    readonly auth: AuthService,
    private readonly notifications: NotificationApiService,
    private readonly sound: NotificationSoundService,
    private readonly stomp: StompService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.refresh();
    this.connectInbox();
  }

  toggleInbox(event: Event): void {
    event.stopPropagation();
    this.open.update((value) => !value);
    if (this.open()) this.refresh();
  }

  @HostListener('document:click')
  closeInbox(): void {
    this.open.set(false);
  }

  openItem(item: AppNotification): void {
    this.notifications.markRead(item.id).subscribe(() => this.refresh());
    this.open.set(false);
    if (item.link) this.router.navigateByUrl(item.link);
  }

  markAll(): void {
    this.notifications.markAllRead().subscribe(() => this.refresh());
  }

  logout(): void {
    this.stomp.disconnect();
    this.auth.logout();
  }

  initials(): string {
    const user = this.auth.currentUser();
    if (!user) return 'U';
    return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
  }

  formatWhen(value: string): string {
    return formatDateTime24(value);
  }

  private refresh(): void {
    this.notifications.list().subscribe((items) => {
      this.items.set(items);
      this.unread.set(items.filter((item) => !item.read).length);
    });
  }

  private connectInbox(): void {
    const user = this.auth.currentUser();
    if (!user || this.subscribedUserId === user.id) return;
    this.subscribedUserId = user.id;
    this.stomp.connect();
    this.stomp.subscribe(`/topic/users/${user.id}/notifications`, (payload) => {
      const notification = payload as AppNotification;
      this.items.update((items) => [notification, ...items.filter((item) => item.id !== notification.id)]);
      this.unread.update((count) => count + (notification.read ? 0 : 1));
      const onThatChat = notification.type === 'CHAT_MESSAGE' && notification.link && this.router.url.startsWith(notification.link);
      if (!onThatChat) this.sound.play();
    });
  }
}
