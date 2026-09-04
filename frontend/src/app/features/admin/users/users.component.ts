import { Component, OnInit, signal } from '@angular/core';
import { UserAdmin } from '../../../core/models/user.model';
import { ConfirmDialogService } from '../../../core/services/confirm-dialog.service';
import { UserAdminService } from '../../../core/services/user-admin.service';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  template: `
    <section class="page-header">
      <h1>Users</h1>
      <p>Manage account status, unlock locked users, and delete accounts.</p>
    </section>
    @if (error()) { <p class="alert error">{{ error() }}</p> }
    <div class="panel table-wrap">
      <table>
        <thead>
          <tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th>Attempts</th><th></th></tr>
        </thead>
        <tbody>
          @for (user of users(); track user.id) {
            <tr>
              <td>{{ user.firstName }} {{ user.lastName }}</td>
              <td>{{ user.email }}</td>
              <td>{{ user.role }}</td>
              <td>
                <span class="badge" [class.badge-success]="user.enabled" [class.badge-danger]="!user.enabled">
                  {{ user.enabled ? 'active' : 'inactive' }}
                </span>
                <span class="badge" [class.badge-warning]="user.accountLocked" [class.badge-success]="!user.accountLocked">
                  {{ user.accountLocked ? 'locked' : 'unlocked' }}
                </span>
              </td>
              <td>{{ user.failedLoginAttempts }}</td>
              <td class="actions">
                @if (user.enabled) {
                  <button class="btn btn-danger" (click)="deactivate(user)">Deactivate</button>
                } @else {
                  <button class="btn btn-light" (click)="activate(user)">Activate</button>
                }
                @if (user.accountLocked) {
                  <button class="btn btn-primary" (click)="unlock(user)">Unlock</button>
                }
                <button class="btn btn-danger" (click)="remove(user)">Delete</button>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `
})
export class UsersComponent implements OnInit {
  users = signal<UserAdmin[]>([]);
  error = signal('');

  constructor(
    private readonly userService: UserAdminService,
    private readonly confirmDialog: ConfirmDialogService
  ) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.userService.list().subscribe({ next: (users) => this.users.set(users), error: () => this.error.set('Failed to load users.') });
  }

  activate(user: UserAdmin): void {
    this.userService.activate(user.id).subscribe({ next: () => this.load(), error: () => this.error.set('Activate failed.') });
  }

  async deactivate(user: UserAdmin): Promise<void> {
    const ok = await this.confirmDialog.confirm({
      title: 'Deactivate user?',
      message: `Deactivate ${user.email}? They will no longer be able to sign in.`,
      confirmLabel: 'Deactivate',
      tone: 'danger'
    });
    if (!ok) return;
    this.userService.deactivate(user.id).subscribe({ next: () => this.load(), error: () => this.error.set('Deactivate failed. Admin cannot deactivate himself.') });
  }

  unlock(user: UserAdmin): void {
    this.userService.unlock(user.id).subscribe({ next: () => this.load(), error: () => this.error.set('Unlock failed.') });
  }

  async remove(user: UserAdmin): Promise<void> {
    const ok = await this.confirmDialog.confirm({
      title: 'Delete account?',
      message: `Delete account ${user.email}? This cannot be undone.`,
      confirmLabel: 'Delete',
      tone: 'danger'
    });
    if (!ok) return;
    this.userService.delete(user.id).subscribe({
      next: () => this.load(),
      error: () => this.error.set('Delete failed. Admin cannot delete himself.')
    });
  }
}
