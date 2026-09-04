import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Formation } from '../../../core/models/catalogue.model';
import { FormationService } from '../../../core/services/formation.service';

@Component({
  selector: 'app-learner-catalogue',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="page-header"><h1>Training catalogue</h1><p>Choose a formation and enroll in an available session.</p></section>
    <div class="card-grid">
      @for (formation of formations(); track formation.id) {
        <article class="item-card">
          <h3>{{ formation.title }}</h3>
          <p>{{ formation.description }}</p>
          <div class="meta">{{ formation.category.name }} · {{ formation.level }} · {{ formation.durationHours }}h</div>
          <a class="btn btn-primary" [routerLink]="['/learner/formations', formation.id]">View details</a>
        </article>
      }
    </div>
  `
})
export class CatalogueComponent implements OnInit {
  formations = signal<Formation[]>([]);
  constructor(private readonly formationsService: FormationService) {}
  ngOnInit(): void { this.formationsService.list({ size: 100, active: true }).subscribe((p) => this.formations.set(p.content)); }
}
