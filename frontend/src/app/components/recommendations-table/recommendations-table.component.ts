import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Nsga2Result, SetSolution } from '../../models/nsga';
import {CommonModule} from "@angular/common";

@Component({
  selector: 'app-recommendations-table',
  styleUrls: ['./recommendations-table.component.css'],
  templateUrl: './recommendations-table.component.html'
})
export class RecommendationsTableComponent {
  @Input() result?: Nsga2Result;
  @Input() selectedIndex: number | null = null;
  @Output() pick = new EventEmitter<number>();

  trackByIdx = (i: number) => i;

  choose(i: number) { this.pick.emit(i); }
  toPos(x: number) { return (-x); } // chuyển objNegPop về Population (dương)
}
