import { Component } from '@angular/core';
import {Nsga2Request, Nsga2Result, SetSolution} from "./models/nsga";
import {DssService} from "./services/dss.service";


@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'Supermarket DSS (NSGA-II)';
  loading = false;
  result?: Nsga2Result;
  selectedIdx: number | null = null;

  constructor(private api: DssService) {}

  onSubmit(req: Nsga2Request) {
    this.loading = true;
    this.selectedIdx = null;
    this.result = undefined;

    this.api.runNsga(req).subscribe({
      next: (res) => {
        this.loading = false;
        this.result = res;
        // chọn phương án #1 mặc định
        if (res.recommendations?.length) this.selectedIdx = 0;
      },
      error: (err) => {
        this.loading = false;
        alert('Lỗi gọi API: ' + (err?.error?.message || err.statusText || 'Unknown'));
        console.error(err);
      }
    });
  }

  onPick(idx: number) {
    this.selectedIdx = idx;
  }

  get selectedSolution(): SetSolution | undefined {
    if (!this.result || this.selectedIdx == null) return undefined;
    return this.result.recommendations?.[this.selectedIdx];
  }
}
