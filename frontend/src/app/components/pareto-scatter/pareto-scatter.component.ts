import { Component, Input, OnChanges } from '@angular/core';
import { BaseChartDirective, NgChartsModule } from 'ng2-charts';
import { ChartConfiguration, ChartOptions } from 'chart.js';
import { CommonModule } from '@angular/common';
import { Nsga2Result } from '../../models/nsga';

@Component({
  selector: 'app-pareto-scatter',
  standalone: true,
  imports: [CommonModule, NgChartsModule],
  templateUrl: './pareto-scatter.component.html'
})
export class ParetoScatterComponent implements OnChanges {
  @Input() result?: Nsga2Result;

  scatterData: ChartConfiguration<'scatter'>['data'] = { datasets: [] };
  scatterOptions: ChartOptions<'scatter'> = {
    responsive: true,
    plugins: { legend: { position: 'bottom' } },
    scales: {
      x: { title: { display: true, text: 'Rent (minimize)' } },
      y: { title: { display: true, text: 'Population (maximize)' } }
    }
  };

  ngOnChanges(): void {
    if (!this.result) { this.scatterData = { datasets: [] }; return; }
    // const datasets = this.result.fronts.map((f, idx) => ({
    //   label: `Front ${idx+1}`,
    //   data: f.individuals.map(p => ({ x: p.objRent, y: -p.objNegPop }))
    // }));
    // this.scatterData = { datasets };
  }
}
