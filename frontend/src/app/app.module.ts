import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

import { AppComponent } from './app.component';
import {NsgaFormComponent} from "./components/nsga-form/nsga-form.component";
import {RecommendationsTableComponent} from "./components/recommendations-table/recommendations-table.component";
import {ParetoMapComponent} from "./components/pareto-map/pareto-map.component";


@NgModule({
    declarations: [
        AppComponent,
        NsgaFormComponent,
        RecommendationsTableComponent,
        ParetoMapComponent
    ],
    imports: [
        BrowserModule,
        FormsModule,
        HttpClientModule
    ],
    bootstrap: [AppComponent]
})
export class AppModule {}
