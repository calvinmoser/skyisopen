import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
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
  imports: [MatToolbarModule, MatButtonModule, MatTableModule, CommonModule, MatTabsModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss', '../animations/home.animations.scss']
})

export class HomeComponent {

  title: string = "Indianapolis International Airport";
  dataSource = new MatTableDataSource<Flight>([]);
  flightMap: Map <String, Flight> = new Map<String, Flight>();
  numPages = 1;
  totalCalls: number = 0;
  displayedColumns: string[] = [ /*"fa_flight_id",*/ "flight", "aircraft_type", /*"scheduled_on",*/ "origin", /*"groundspeed",*/
    /*"altitude", "angle",*/ "to_airport", /*"to_waypoint", "estimated", "next_update", "updated", "remove"*/ "estimated_on"];

  aircraftTypes: Map <String, number> = new Map<String, number>();
  typeDataSource = new MatTableDataSource<[String, number]>([]);
  flights: Flight[] = [];

  bigPlanes: string[] = ["A34", "A35", "A36", "A38", "B74", "B77", "B78"];

  easterEgg: boolean = false;
  easterEggCount: number = 0;

  constructor(private aeroAPIservice: AeroAPIService) {}

  ngOnInit(): void {
    if (window.innerWidth < 365) {
      this.title = "Indianapolis"
    } else if (window.innerWidth < 440) {
      this.title = "Indianapolis International"
    }
    if (window.innerWidth > window.innerHeight && window.innerWidth >- 1024) {
      this.numPages = 4;
    }
    this.getScheduledArrivals(this.numPages);
  }

  getScheduledArrivals(maxPages: number) {
    this.aeroAPIservice.getScheduledArrivals(maxPages)
      .subscribe(flights => {
        for (var flight of flights) {
          flight.calcInitialDistance();

          if (flight.operator == null) flight.operator = "???";
          if (flight.aircraft_type == null) flight.aircraft_type = "???";

          if (this.aircraftTypes.has(flight.aircraft_type)) {
            var num = this.aircraftTypes.get(flight.aircraft_type);
            this.aircraftTypes.set(flight.aircraft_type, num! + 1);
          } else
            this.aircraftTypes.set(flight.aircraft_type, 1)

          for (var type of this.bigPlanes) {
            if (flight.aircraft_type.startsWith(type)) {
              flight.color = "track";
            }
          }
        }
        this.flights = flights;
        this.dataSource.data = flights;
        this.typeDataSource.data = [...this.aircraftTypes].sort((a, b) => a[0].valueOf().localeCompare(b[0].valueOf()));
      });
  }

  identifyAircraft(){
    this.aeroAPIservice.getScheduledArrivals(4)
      .subscribe(async flights => {
        flights.map(f => {f.calcInitialDistance()});
        this.flights = flights;
        this.dataSource.data = flights;
        var foundOne = false;
        flights = flights.sort((a, b) => {return a.to_airport - b.to_airport});
        for (var i = 0; i < flights.length; i++) {
          var flight = flights[i];
          var position = await this.aeroAPIservice.getFlightPosition(flight.fa_flight_id);

          if (typeof position == "undefined" || position.last_position == null) continue;

          flight.last_position = position.last_position;

          var to_waypoint = flight.calcDistance(Airport.finalWP, position.last_position);
          flight.to_waypoint = to_waypoint;

          var to_airport = flight.calcDistance(Airport.position, position.last_position);
          flight.to_airport = to_airport;

          if (to_waypoint < 2) {
            flight.color = "target"; // In target zone
            setTimeout(() => {flights.map((f) => {f.color = "mat-row"; return f;})}, 10000);
            foundOne = true;
          }

          if (i > 5 || (foundOne && to_waypoint > 10)) return; // Out of bounds
        }
      });
  }

  removeFlight(flight: Flight) {};

  easterEggHunt() {
    console.log(this.easterEggCount);
    this.easterEggCount++;
    if (this.easterEggCount >= 7) {
      this.easterEgg = !this.easterEgg;
      this.easterEggCount = 0;
    }
  }

  openFlightRadar24(flight: Flight){
    let url = "https://www.flightradar24.com/" + flight.getFlight();
    window.open(url, "_blank", "noreferrer");
  }
}
