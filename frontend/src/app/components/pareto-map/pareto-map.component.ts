import { AfterViewInit, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import * as L from 'leaflet';
import { SetSolution, Site } from '../../models/nsga';

@Component({
    selector: 'app-pareto-map',
    templateUrl: './pareto-map.component.html',
    styleUrls: ['./pareto-map.component.css']
})
export class ParetoMapComponent implements AfterViewInit, OnChanges {
    @Input() solution?: SetSolution;

    private map?: L.Map;
    private layerGroup?: L.LayerGroup;
    private defaultCenter: L.LatLngExpression = [21.0278, 105.8342]; // Hà Nội
    private defaultZoom = 12;



    ngAfterViewInit(): void {
        const DefaultIcon = L.icon({
            iconRetinaUrl: 'assets/marker-icon-2x.png',
            iconUrl:       'assets/marker-icon.png',
            shadowUrl:     'assets/marker-shadow.png',
            iconSize:      [25, 41],
            iconAnchor:    [12, 41],
            popupAnchor:   [1, -34],
            tooltipAnchor: [16, -28],
            shadowSize:    [41, 41]
        });
        L.Marker.prototype.options.icon = DefaultIcon;

        this.map = L.map('map', {
            center: this.defaultCenter,
            zoom: this.defaultZoom
        });

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; OpenStreetMap'
        }).addTo(this.map);

        this.layerGroup = L.layerGroup().addTo(this.map);
        this.renderMarkers();
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['solution']) {
            this.renderMarkers();
        }
    }

    private renderMarkers() {
        if (!this.map || !this.layerGroup) return;
        this.layerGroup.clearLayers();

        const sites: Site[] = this.solution?.sites ?? [];
        if (!sites.length) return;

        const bounds = L.latLngBounds([]);

        sites.forEach((s, idx) => {
            const marker = L.marker([s.lat, s.lng], {
                title: `${idx+1}. ${s.name ?? 'Lô ' + s.candidateId}`
            }).bindPopup(this.popupHtml(s, idx+1));
            marker.addTo(this.layerGroup!);
            bounds.extend([s.lat, s.lng]);
        });

        // Fit bounds
        if (bounds.isValid()) {
            this.map.fitBounds(bounds.pad(0.2));
        }
    }

    private popupHtml(s: Site, order: number): string {
        return `
      <div>
        <b>${order}. ${s.name ?? 'Lô ' + s.candidateId}</b><br/>
        ID: ${s.candidateId}<br/>
        Địa chỉ: ${s.address ?? '-'}<br/>
        Thuê: ${this.formatNumber(s.rent)} VND/tháng<br/>
        Dân số ~ ${this.formatNumber(s.pop1km)}<br/>
        Đối thủ: ${s.comp500m}
      </div>
    `;
    }

    private formatNumber(v?: number) {
        return (v ?? 0).toLocaleString('vi-VN');
    }
}
