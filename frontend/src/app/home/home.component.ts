import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { AeroAPIService } from '../services/aeroapi.service';
import { Flight, Position } from '../model/flight';

// NOTE: Perhaps since this is a standalone component, the BrowserAnimationsModule needs to be added
//  as a provider in app.config: provideAnimations();
import { MatTabsModule } from '@angular/material/tabs';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [MatCheckboxModule, MatTableModule, CommonModule, MatTabsModule, ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})

export class HomeComponent {

  dataSource = new MatTableDataSource<Flight>([]);
  flightMap: Map <String, Flight> = new Map<String, Flight>();
  totalCalls: number = 0;
  displayedColumns: string[] = [ "fa_flight_id", "flight", "aircraft_type", "estimated_on", "origin", "groundspeed",
    "altitude", "angle", "to_airport", "to_waypoint", "estimated", "next_update", "updated", /*"remove"*/ ];

  aircraftTypes: Map <String, number> = new Map<String, number>();
  typeDataSource = new MatTableDataSource<[String, number]>([]);

  constructor(private aeroAPIservice: AeroAPIService) {}

  ngOnInit(): void {
    this.aeroAPIservice.getScheduledArrivals()
      .subscribe(flights => {
        for (var flight of flights) {
          flight.calcInitialDistance();
          if (this.aircraftTypes.has(flight.aircraft_type)) {
            var num = this.aircraftTypes.get(flight.aircraft_type);
            this.aircraftTypes.set(flight.aircraft_type, num! + 1);
          } else
            this.aircraftTypes.set(flight.aircraft_type, 1)
        }
        this.dataSource.data = flights;
        this.typeDataSource.data = [...this.aircraftTypes].sort((a, b) => a[0].valueOf().localeCompare(b[0].valueOf()));
      });
  }

  removeFlight(flight: Flight) {};

}
