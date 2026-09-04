import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LearningPath } from '../../../core/models/learning-path.model';
import { LearnerProfileScore } from '../../../core/models/profile.model';
import { LearningPathService } from '../../../core/services/learning-path.service';
import { LearnerService } from '../../../core/services/learner.service';

@Component({
  selector: 'app-learner-dashboard',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="hero-panel">
      <div>
        <span class="eyebrow">Learner space</span>
        <h1>Build skills through professional training</h1>
        <p>Browse the catalogue, enroll in sessions, and follow your learning path.</p>
        <a class="btn btn-primary" routerLink="/learner/catalogue">Explore catalogue</a>
      </div>
      <img src="assets/template/img/girl-laptop.png" alt="Learning">
    </section>
    <section class="panel profile-intelligence">
      <div>
        <span class="eyebrow">Profile intelligence</span>
        <h2>Profile completion</h2>
      </div>
      @if (score(); as s) {
        <div class="progress-track"><span [style.width.%]="s.score"></span></div>
        <strong>{{ s.score }}%</strong>
        <p>{{ s.message }}</p>
        @if (s.missingFields.length) {
          <p class="muted">Missing: {{ s.missingFields.join(', ') }}</p>
        }
      }
      <a class="btn btn-primary" routerLink="/learner/improvement-plan">View improvement plan</a>
    </section>
    <section class="panel profile-intelligence">
      <div>
        <span class="eyebrow">Your learning path</span>
        <h2>Path completion</h2>
      </div>
      @if (learningPath(); as p) {
        <div class="progress-track"><span [style.width.%]="p.globalProgressPercentage"></span></div>
        <strong>{{ p.globalProgressPercentage }}%</strong>
        <p>{{ p.completedSteps }}/{{ p.totalSteps }} steps completed · {{ p.estimatedTotalHours }}h estimated</p>
        @if (p.nextRecommendedFormationTitle) {
          <p class="muted">Next: {{ p.nextRecommendedFormationTitle }}</p>
        }
      }
      <a class="btn btn-primary" routerLink="/learner/learning-path">View learning path</a>
    </section>
  `
})
export class LearnerDashboardComponent implements OnInit {
  score = signal<LearnerProfileScore | null>(null);
  learningPath = signal<LearningPath | null>(null);

  constructor(private readonly learnerService: LearnerService, private readonly learningPathService: LearningPathService) {}

  ngOnInit(): void {
    this.learnerService.profileScore().subscribe((score) => this.score.set(score));
    this.learningPathService.mine().subscribe((path) => this.learningPath.set(path));
  }
}
