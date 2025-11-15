import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Nsga2Request} from "../../models/nsga";
import {City} from "../../models/city";
import {DssService} from "../../services/dss.service";

@Component({
  selector: 'app-nsga-form',
  templateUrl: './nsga-form.component.html',
  styleUrls: ['./nsga-form.component.css']
})
export class NsgaFormComponent {
  @Input() loading = false;
  @Output() submitReq = new EventEmitter<Nsga2Request>();

  model: Nsga2Request = {
    kLots: 3,
    pickTopSets: 5,
    populationRadius: 1000,
    competitionRadius: 500,
    minDistanceMeters: 300,
    distancePenalty: 0.05,
    preset: 'balanced',
    maxRent: undefined,
    availableFromIso: undefined,
    candidateLimit: 200,
    cityIds: undefined
  };

  cities: City[] = [];
  selectedCityIds: number[] = [];


  constructor(private api: DssService) {}


  ngOnInit(): void {
    this.api.getCities().subscribe({
      next: (cs) => this.cities = cs ?? [],
      error: (e) => console.error('Load cities failed', e)
    });
  }

  selectAllCities() {
    this.selectedCityIds = this.cities.map(c => c.id);
  }
  clearAllCities() {
    this.selectedCityIds = [];
  }

  removeItem(id: number | string) {
    this.selectedCityIds = this.selectedCityIds.filter(x => x !== id);
  }
  run() {
    if (!this.model.kLots || this.model.kLots < 1) return;
    if (!this.model.pickTopSets || this.model.pickTopSets < 1) return;
    this.model.cityIds = [...this.selectedCityIds];
    this.submitReq.emit({ ...this.model });
  }
}
