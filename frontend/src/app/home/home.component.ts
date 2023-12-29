import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { AeroAPIService } from '../services/aeroapi.service';
import { Flight, Position } from '../model/flight';
import { Airport } from '../model/airport';

// NOTE: Perhaps since this is a standalone component, the BrowserAnimationsModule needs to be added
//  as a provider in app.config: provideAnimations();
import { MatTabsModule } from '@angular/material/tabs';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [MatButtonModule, MatTableModule, CommonModule, MatTabsModule, ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})

export class HomeComponent {

  dataSource = new MatTableDataSource<Flight>([]);
  flightMap: Map <String, Flight> = new Map<String, Flight>();
  totalCalls: number = 0;
  displayedColumns: string[] = [ "fa_flight_id", "flight", "aircraft_type", "estimated_on", "origin", "groundspeed",
    "altitude", /*"angle",*/ "to_airport", "to_waypoint", /*"estimated", "next_update", "updated", "remove"*/ ];

  aircraftTypes: Map <String, number> = new Map<String, number>();
  typeDataSource = new MatTableDataSource<[String, number]>([]);
  flights: Flight[] = [];

  constructor(private aeroAPIservice: AeroAPIService) {}

  ngOnInit(): void {
    this.getScheduledArrivals(4);
  }

  getScheduledArrivals(maxPages: number) {
    this.aeroAPIservice.getScheduledArrivals(maxPages)
      .subscribe(flights => {
        for (var flight of flights) {
          flight.calcInitialDistance();
          if (this.aircraftTypes.has(flight.aircraft_type)) {
            var num = this.aircraftTypes.get(flight.aircraft_type);
            this.aircraftTypes.set(flight.aircraft_type, num! + 1);
          } else
            this.aircraftTypes.set(flight.aircraft_type, 1)
        }
        this.flights = flights;
        this.dataSource.data = flights;
        this.typeDataSource.data = [...this.aircraftTypes].sort((a, b) => a[0].valueOf().localeCompare(b[0].valueOf()));
      });
  }

  identifyAircraft(){

    this.aeroAPIservice.getScheduledArrivals(1)
      .subscribe(async flights => {
        this.flights = flights;
        this.dataSource.data = flights;
        for (var flight of this.flights) {
          var position = await this.aeroAPIservice.getFlightPosition(flight.fa_flight_id);

          if (typeof position == "undefined" || position.last_position == null) continue;

          flight.last_position = position.last_position;

          var to_waypoint = flight.calcDistance(Airport.finalWP, position.last_position);
          flight.to_waypoint = "" + to_waypoint;

          var to_airport = flight.calcDistance(Airport.position, position.last_position);
          flight.to_airport = "" + to_airport;

          if (to_waypoint < 2) flight.color = "target"; // In target zone

          if (to_airport > 10) return; // Out of bounds
        }
      });
  }

  removeFlight(flight: Flight) {};

}
