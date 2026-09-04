import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { Role } from '../models/user.model';

export const roleGuard: CanActivateFn = (route) => {
  const roles = route.data['roles'] as Role[] | undefined;
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!roles || auth.hasRole(roles)) {
    return true;
  }

  auth.redirectAfterLogin();
  return false;
};
