import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { AdminLayoutComponent } from './layout/admin-layout/admin-layout.component';
import { LearnerLayoutComponent } from './layout/learner-layout/learner-layout.component';
import { TrainerLayoutComponent } from './layout/trainer-layout/trainer-layout.component';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { ForgotPasswordComponent } from './features/auth/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './features/auth/reset-password/reset-password.component';
import { ChangePasswordComponent } from './features/auth/change-password/change-password.component';
import { AdminDashboardComponent } from './features/admin/dashboard/dashboard.component';
import { CategoriesComponent } from './features/admin/categories/categories.component';
import { FormationsComponent } from './features/admin/formations/formations.component';
import { TrainersComponent } from './features/admin/trainers/trainers.component';
import { SessionsComponent } from './features/admin/sessions/sessions.component';
import { EnrollmentsComponent } from './features/admin/enrollments/enrollments.component';
import { UsersComponent } from './features/admin/users/users.component';
import { TrainerWorkloadComponent } from './features/admin/trainer-workload/trainer-workload.component';
import { AdminMlaComponent } from './features/admin/mla/admin-mla.component';
import { LearnerDashboardComponent } from './features/learner/dashboard/dashboard.component';
import { CatalogueComponent } from './features/learner/catalogue/catalogue.component';
import { FormationDetailComponent } from './features/learner/formation-detail/formation-detail.component';
import { MyEnrollmentsComponent } from './features/learner/my-enrollments/my-enrollments.component';
import { LearnerProfileComponent } from './features/learner/profile/profile.component';
import { ImprovementPlanComponent } from './features/learner/improvement-plan/improvement-plan.component';
import { LearningPathComponent } from './features/learner/learning-path/learning-path.component';
import { TrainerDashboardComponent } from './features/trainer/dashboard/dashboard.component';
import { MySessionsComponent } from './features/trainer/my-sessions/my-sessions.component';
import { TrainerProfileComponent } from './features/trainer/profile/profile.component';
import { AvailabilityComponent } from './features/trainer/availability/availability.component';
import { SessionChatComponent } from './features/chat/session-chat.component';
import { SessionClassroomComponent } from './features/classroom/session-classroom.component';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: AdminDashboardComponent },
      { path: 'categories', component: CategoriesComponent },
      { path: 'formations', component: FormationsComponent },
      { path: 'chapters', redirectTo: 'formations', pathMatch: 'full' },
      { path: 'trainers', component: TrainersComponent },
      { path: 'sessions', component: SessionsComponent },
      { path: 'enrollments', component: EnrollmentsComponent },
      { path: 'trainer-workload', component: TrainerWorkloadComponent },
      { path: 'users', component: UsersComponent },
      { path: 'mla', component: AdminMlaComponent },
      { path: 'change-password', component: ChangePasswordComponent }
    ]
  },
  {
    path: 'learner',
    component: LearnerLayoutComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['LEARNER'] },
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: LearnerDashboardComponent },
      { path: 'catalogue', component: CatalogueComponent },
      { path: 'formations/:id', component: FormationDetailComponent },
      { path: 'my-enrollments', component: MyEnrollmentsComponent },
      { path: 'sessions/:sessionId/chat', component: SessionChatComponent },
      { path: 'sessions/:sessionId/classroom', component: SessionClassroomComponent },
      { path: 'improvement-plan', component: ImprovementPlanComponent },
      { path: 'learning-path', component: LearningPathComponent },
      { path: 'profile', component: LearnerProfileComponent },
      { path: 'change-password', component: ChangePasswordComponent }
    ]
  },
  {
    path: 'trainer',
    component: TrainerLayoutComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['TRAINER'] },
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: TrainerDashboardComponent },
      { path: 'my-sessions', component: MySessionsComponent },
      { path: 'sessions/:sessionId/chat', component: SessionChatComponent },
      { path: 'sessions/:sessionId/classroom', component: SessionClassroomComponent },
      { path: 'profile', component: TrainerProfileComponent },
      { path: 'availability', component: AvailabilityComponent },
      { path: 'change-password', component: ChangePasswordComponent }
    ]
  },
  { path: '**', redirectTo: 'login' }
];
