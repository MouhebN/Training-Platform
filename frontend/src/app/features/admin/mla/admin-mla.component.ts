import { Component, OnInit, computed, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { AdminMlService } from '../../../core/services/admin-ml.service';
import { ConfirmDialogService } from '../../../core/services/confirm-dialog.service';

@Component({
  selector: 'app-admin-mla',
  standalone: true,
  imports: [DecimalPipe],
  styles: [`
    :host { display: block; max-width: 1100px; }
    .stat-grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 12px;
      margin-bottom: 18px;
    }
    .stat {
      background: #fff;
      border: 1px solid var(--line);
      border-radius: 12px;
      padding: 14px 16px;
    }
    .stat span { display: block; color: var(--muted); font-size: 12px; margin-bottom: 4px; }
    .stat strong { font-size: 22px; }
    .phase-list { margin: 0; padding-left: 18px; display: grid; gap: 6px; }
    .importance { display: grid; gap: 8px; }
    .importance-row { display: grid; grid-template-columns: 180px 1fr 48px; gap: 10px; align-items: center; }
    .bar {
      height: 10px;
      background: #f0f0f0;
      border-radius: 999px;
      overflow: hidden;
    }
    .bar > i {
      display: block;
      height: 100%;
      background: var(--primary);
    }
    .actions { display: flex; flex-wrap: wrap; gap: 10px; margin: 12px 0 18px; }
    .sample-wrap { overflow: auto; }
    @media (max-width: 900px) {
      .stat-grid { grid-template-columns: 1fr 1fr; }
      .importance-row { grid-template-columns: 1fr; }
    }
  `],
  template: `
    <section class="page-header">
      <h1>MLA Center</h1>
      <p>Profile analysis → formation suggestions (Python RandomForest). Demo the full ML pipeline from the UI.</p>
    </section>

    @if (error()) { <p class="alert error">{{ error() }}</p> }
    @if (message()) { <p class="alert success">{{ message() }}</p> }

    <div class="actions">
      <button class="btn btn-light" type="button" (click)="refresh()" [disabled]="loading()">Refresh</button>
      <button class="btn btn-primary" type="button" (click)="retrain()" [disabled]="loading() || retraining()">
        {{ retraining() ? 'Training RandomForest…' : 'Train / retrain model' }}
      </button>
    </div>

    <div class="stat-grid">
      <div class="stat">
        <span>Service</span>
        <strong>{{ serviceStatus() }}</strong>
      </div>
      <div class="stat">
        <span>Dataset rows</span>
        <strong>{{ datasetRows() }}</strong>
      </div>
      <div class="stat">
        <span>Accuracy</span>
        <strong>{{ accuracyLabel() }}</strong>
      </div>
      <div class="stat">
        <span>F1 score</span>
        <strong>{{ f1Label() }}</strong>
      </div>
    </div>

    <section class="panel">
      <h2>Pipeline phases</h2>
      <ol class="phase-list">
        @for (phase of phases(); track phase) {
          <li>{{ phase }}</li>
        } @empty {
          <li class="muted">Load pipeline to see phases.</li>
        }
      </ol>
      <p class="muted" style="margin-top:12px;">
        Algorithm: <strong>{{ algorithm() }}</strong>
        @if (nEstimators()) { · {{ nEstimators() }} trees }
      </p>
    </section>

    <section class="panel">
      <h2>Feature importance</h2>
      <div class="importance">
        @for (item of featureImportances(); track item.name) {
          <div class="importance-row">
            <span>{{ item.name }}</span>
            <div class="bar"><i [style.width.%]="item.value * 100"></i></div>
            <strong>{{ (item.value * 100) | number:'1.1-1' }}%</strong>
          </div>
        } @empty {
          <p class="muted">No model loaded yet.</p>
        }
      </div>
    </section>

    <section class="panel table-wrap sample-wrap">
      <h2>Dataset sample</h2>
      <p class="muted">
        {{ datasetRows() }} rows
        · positives {{ datasetPositives() }}
        · negatives {{ datasetNegatives() }}
      </p>
      <table>
        <thead>
          <tr>
            <th>Level</th>
            <th>Skills</th>
            <th>Goals</th>
            <th>Formation</th>
            <th>Label</th>
          </tr>
        </thead>
        <tbody>
          @for (row of sampleRows(); track $index) {
            <tr>
              <td>{{ row['learner_level'] }}</td>
              <td>{{ row['learner_skills'] }}</td>
              <td>{{ row['learner_goals'] }}</td>
              <td>{{ row['formation_title'] }}</td>
              <td>{{ row['label'] }}</td>
            </tr>
          } @empty {
            <tr><td colspan="5" class="muted">No sample loaded.</td></tr>
          }
        </tbody>
      </table>
    </section>
  `
})
export class AdminMlaComponent implements OnInit {
  pipeline = signal<Record<string, any> | null>(null);
  sample = signal<Record<string, any> | null>(null);
  loading = signal(false);
  retraining = signal(false);
  error = signal('');
  message = signal('');

  phases = computed(() => (this.pipeline()?.['phases'] as string[]) ?? []);
  algorithm = computed(() => (this.pipeline()?.['model'] as any)?.algorithm ?? '—');
  nEstimators = computed(() => (this.pipeline()?.['model'] as any)?.nEstimators ?? null);
  serviceStatus = computed(() => {
    const model = this.pipeline()?.['model'] as any;
    if (!this.pipeline()) return '…';
    return model?.loaded ? 'UP' : 'DOWN';
  });
  datasetRows = computed(() => (this.pipeline()?.['dataset'] as any)?.rows ?? 0);
  datasetPositives = computed(() => (this.pipeline()?.['dataset'] as any)?.positiveLabels ?? 0);
  datasetNegatives = computed(() => (this.pipeline()?.['dataset'] as any)?.negativeLabels ?? 0);
  accuracyLabel = computed(() => {
    const value = (this.pipeline()?.['training'] as any)?.accuracy;
    return value == null ? '—' : `${(Number(value) * 100).toFixed(1)}%`;
  });
  f1Label = computed(() => {
    const value = (this.pipeline()?.['training'] as any)?.f1;
    return value == null ? '—' : Number(value).toFixed(3);
  });
  featureImportances = computed(() => {
    const raw = ((this.pipeline()?.['model'] as any)?.featureImportances ?? {}) as Record<string, number>;
    return Object.entries(raw)
      .map(([name, value]) => ({ name, value: Number(value) || 0 }))
      .sort((a, b) => b.value - a.value);
  });
  sampleRows = computed(() => ((this.sample()?.['sample'] as Record<string, string>[]) ?? []));

  constructor(private readonly ml: AdminMlService, private readonly confirmDialog: ConfirmDialogService) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set('');
    this.ml.pipeline().subscribe({
      next: (pipeline) => {
        this.pipeline.set(pipeline);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('MLA service unavailable. Start Python on port 8000 (uvicorn).');
        this.loading.set(false);
      }
    });
    this.ml.datasetSample(10).subscribe({
      next: (sample) => this.sample.set(sample),
      error: () => this.sample.set(null)
    });
  }

  async retrain(): Promise<void> {
    const ok = await this.confirmDialog.confirm({
      title: 'Retrain MLA model?',
      message: 'This will retrain the RandomForest model on the current dataset. It can take about 10–30 seconds.',
      confirmLabel: 'Train model',
      tone: 'primary'
    });
    if (!ok) return;
    this.retraining.set(true);
    this.error.set('');
    this.message.set('');
    this.ml.retrain().subscribe({
      next: (result) => {
        this.retraining.set(false);
        const metrics = (result['metrics'] as any) ?? {};
        this.message.set(
          `Model retrained. Accuracy ${(Number(metrics.accuracy ?? 0) * 100).toFixed(1)}% · F1 ${Number(metrics.f1 ?? 0).toFixed(3)}`
        );
        this.refresh();
      },
      error: () => {
        this.retraining.set(false);
        this.error.set('Retrain failed. Check that the MLA service is running.');
      }
    });
  }
}
