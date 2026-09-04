import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TrainerWorkload } from '../../../core/models/planning.model';
import { PlanningService } from '../../../core/services/planning.service';

@Component({
  selector: 'app-trainer-workload',
  standalone: true,
  imports: [FormsModule],
  template: `
    <section class="page-header">
      <h1>Trainer Workload</h1>
      <p>Balance trainer assignments and detect overloaded schedules.</p>
    </section>

    <section class="panel form-row">
      <label>From <input type="date" [(ngModel)]="from"></label>
      <label>To <input type="date" [(ngModel)]="to"></label>
      <button class="btn btn-primary" (click)="load()">Apply filter</button>
    </section>

    <section class="card-grid">
      @for (trainer of workloads(); track trainer.trainerId) {
        <article class="item-card">
          <div class="inline spread">
            <h3>{{ trainer.trainerFullName }}</h3>
            <span class="badge" [class.badge-success]="trainer.workloadLevel === 'LOW' || trainer.workloadLevel === 'NORMAL'" [class.badge-warning]="trainer.workloadLevel === 'HIGH'" [class.badge-danger]="trainer.workloadLevel === 'OVERLOADED'">
              {{ trainer.workloadLevel }}
            </span>
          </div>
          <div class="meta">{{ trainer.trainerEmail }}</div>
          <div class="progress-track"><span [style.width.%]="progress(trainer.totalHours)"></span></div>
          <strong>{{ trainer.totalHours }} hours · {{ trainer.sessionCount }} sessions</strong>
          <p>{{ trainer.recommendation }}</p>
          <p class="muted">{{ trainer.upcomingSessions }} upcoming · {{ trainer.completedSessions }} completed</p>
          <div class="list-row">
            @for (session of trainer.sessions; track session.sessionId) {
              <div>
                <strong>{{ session.sessionTitle }}</strong>
                <div class="meta">{{ session.formationTitle }} · {{ session.status }} · {{ session.durationHours }}h</div>
              </div>
            } @empty {
              <p class="muted">No sessions in this period.</p>
            }
          </div>
        </article>
      }
    </section>
  `
})
export class TrainerWorkloadComponent implements OnInit {
  workloads = signal<TrainerWorkload[]>([]);
  from = new Date().toISOString().slice(0, 10);
  to = new Date(new Date().setDate(new Date().getDate() + 30)).toISOString().slice(0, 10);

  constructor(private readonly planningService: PlanningService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.planningService.workload(this.from, this.to).subscribe((data) => this.workloads.set(data));
  }

  progress(hours: number): number {
    return Math.min(100, Math.round((hours / 35) * 100));
  }
}
