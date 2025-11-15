import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Nsga2Request, Nsga2Result } from '../models/nsga';
import { environment } from '../../environments/environment';
import {City} from "../models/city";

@Injectable({ providedIn: 'root' })
export class DssService {
  private base = environment.apiBase;
  constructor(private http: HttpClient) {}
  runNsga(req: Nsga2Request): Observable<Nsga2Result> {
    return this.http.post<Nsga2Result>(`${this.base}/nsga/run`, req);
  }

  getCities() {
    return this.http.get<City[]>(`${this.base}/city`);
  }
}
