import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { AeroAPIService } from '../services/aeroapi.service';
import { AuthService } from '../services/auth.service';
import { LoginDialogComponent } from '../login-dialog/login-dialog.component';
import { Flight, Position } from '../model/flight';
import { Airport } from '../model/airport';
import { NGXLogger } from 'ngx-logger';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [MatToolbarModule, MatButtonModule, MatTableModule, CommonModule, MatTabsModule, LoginDialogComponent],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss', '../animations/home.animations.scss']
})

export class HomeComponent {

  title: string = "Indianapolis International Airport";
  dataSource = new MatTableDataSource<Flight>([]);
  flightMap: Map <String, Flight> = new Map<String, Flight>();
  numPages = 2;
  totalCalls: number = 0;
  displayedColumns: string[] = [ /*"fa_flight_id",*/ "flight", "aircraft_type", /*"scheduled_on",*/ "origin", /*"groundspeed",*/
    /*"altitude", "angle",*/ "to_airport", /*"to_waypoint", "estimated", "next_update", "updated", "remove"*/ "estimated_on"];

  aircraftTypes: Map <String, number> = new Map<String, number>();
  typeDataSource = new MatTableDataSource<[String, number]>([]);
  flights: Flight[] = [];

  bigPlanes: string[] = ["A34", "A35", "A36", "A38", "B74", "B77", "B78"];

  easterEgg: boolean = false;
  easterEggCount: number = 0;
  planeFlyover: boolean = false;
  flyoverDuration: number = 10;
  flyoverDelay: number = 0;

  private nearFlights: Set<string> = new Set();
  private nearPollInterval: any;

  constructor(private aeroAPIservice: AeroAPIService, public authService: AuthService, private dialog: MatDialog, private snackBar: MatSnackBar, private logger: NGXLogger) {}

  ngOnInit(): void {
    if (window.innerWidth < 365) {
      this.title = "Indianapolis"
    } else if (window.innerWidth < 440) {
      this.title = "Indianapolis International"
    }
    if (window.innerWidth > window.innerHeight && window.innerWidth >- 1024) {
      this.numPages = 1;
    }
    this.getScheduledArrivals(this.numPages);
  }

  getScheduledArrivals(maxPages: number) {
    this.aeroAPIservice.getScheduledArrivals(maxPages)
      .subscribe({ next: (flights) => {
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
        if (!this.authService.isAuthenticated()) this.initFlyoverTracking();
      }, error: (err) => {
        console.error('getScheduledArrivals failed:', err);
        this.snackBar.open('Failed to load flights.', undefined, { duration: 4000, verticalPosition: 'top' });
      }});
  }

  identifyAircraft(){
    this.logger.debug('[IDENTIFY] Button pressed');
    this.aeroAPIservice.getScheduledArrivals(this.numPages)
      .subscribe(async flights => {
        flights.map(f => {f.calcInitialDistance()});
        for (const flight of flights) {
          for (const type of this.bigPlanes) {
            if (flight.aircraft_type.startsWith(type)) flight.color = "track";
          }
        }
        this.flights = flights;
        this.dataSource.data = flights;
        var foundOne = false;
        flights = flights.sort((a, b) => {return a.to_airport - b.to_airport});
        this.logger.debug(`[IDENTIFY] Checking ${flights.length} flights sorted by to_airport`);
        for (var i = 0; i < flights.length; i++) {
          var flight = flights[i];
          var position = await this.aeroAPIservice.getFlightPosition(flight.fa_flight_id);

          if (typeof position == "undefined" || position.last_position == null) {
            this.logger.debug(`[IDENTIFY] [${i}] ${flight.fa_flight_id} — no position, skipping`);
            continue;
          }

          flight.last_position = position.last_position;
          this.logger.debug(`[IDENTIFY] [${i}] ${flight.fa_flight_id} — position: lat=${position.last_position.latitude}, lng=${position.last_position.longitude}`);

          var to_waypoint = flight.calcDistance(Airport.finalWP, position.last_position);
          flight.to_waypoint = to_waypoint;

          var to_airport = flight.calcDistance(Airport.position, position.last_position);
          this.logger.debug(`[IDENTIFY] [${i}] ${flight.fa_flight_id} — to_airport BEFORE: ${flight.to_airport}`);
          flight.to_airport = to_airport;
          this.logger.debug(`[IDENTIFY] [${i}] ${flight.fa_flight_id} — to_airport AFTER: ${flight.to_airport}`);
          this.dataSource.data = this.flights;

          this.logger.debug(`[IDENTIFY] [${i}] ${flight.fa_flight_id} — to_waypoint: ${to_waypoint.toFixed(2)} mi, to_airport: ${to_airport.toFixed(2)} mi`);

          if (to_waypoint < 2) {
            this.logger.debug(`[IDENTIFY] [${i}] ${flight.fa_flight_id} — IN TARGET ZONE (to_waypoint < 2), setting color=target`);
            flight.color = "target"; // In target zone
            setTimeout(() => {flights.map((f) => {f.color = "mat-row"; return f;})}, 10000);
            foundOne = true;
          }

          if (i > 5 || (foundOne && to_waypoint > 10)) {
            this.logger.debug(`[IDENTIFY] Stopping early — i=${i}, foundOne=${foundOne}, to_waypoint=${to_waypoint.toFixed(2)}`);
            return;
          }
        }
        this.logger.debug('[IDENTIFY] Finished all flights, foundOne=' + foundOne);
      });
  }

  openLogin() {
    const ref = this.dialog.open(LoginDialogComponent);
    ref.afterClosed().subscribe(result => {
      if (result) {
        this.authService.login(result.username, result.password);
        this.getScheduledArrivals(this.numPages);
      }
    });
  }

  logout() {
    this.authService.logout();
    this.getScheduledArrivals(this.numPages);
  }

  removeFlight(flight: Flight) {};

  triggerPlane(duration: number = 10, to_waypoint: number = 4) {
    if (this.planeFlyover) {
      this.logger.debug(`[FLYOVER] triggerPlane called but animation already active, skipping (to_waypoint=${to_waypoint.toFixed(2)}, duration=${duration}s)`);
      return;
    }
    this.flyoverDuration = duration;
    const elapsed = (4 - Math.min(to_waypoint, 4)) / 8 * duration;
    this.flyoverDelay = -elapsed;
    const remaining = duration - elapsed;
    this.logger.debug(`[FLYOVER] Triggering animation — to_waypoint=${to_waypoint.toFixed(2)} mi, duration=${duration}s, elapsed=${elapsed.toFixed(1)}s, remaining=${remaining.toFixed(1)}s`);
    this.planeFlyover = true;
    setTimeout(() => this.planeFlyover = false, remaining * 1000);
  }

  async initFlyoverTracking() {
    clearInterval(this.nearPollInterval);
    this.nearFlights.clear();

    const top10 = [...this.flights].slice(0, 10);
    for (const flight of top10) {
      const position = await this.aeroAPIservice.getFlightPosition(flight.fa_flight_id);
      if (!position?.last_position) continue;
      this.scheduleFlight(flight, position.last_position);
    }

    this.nearPollInterval = setInterval(() => this.pollNearFlights(), 5 * 60 * 1000);
  }

  scheduleFlight(flight: Flight, lastPosition: any) {
    const to_waypoint = flight.calcDistance(Airport.finalWP, lastPosition);
    const groundspeedMph = lastPosition.groundspeed * 1.15078;

    this.logger.debug(`[SCHEDULE] ${flight.fa_flight_id} — to_waypoint=${to_waypoint.toFixed(2)} mi, groundspeed=${groundspeedMph.toFixed(0)} mph`);

    if (to_waypoint < 4) {
      const duration = Math.round(8 / groundspeedMph * 3600);
      this.logger.debug(`[SCHEDULE] ${flight.fa_flight_id} — IN RANGE (<4 mi), triggering animation (duration=${duration}s)`);
      this.triggerPlane(duration, to_waypoint);
    } else if (to_waypoint < 20) {
      this.logger.debug(`[SCHEDULE] ${flight.fa_flight_id} — NEAR (<20 mi), added to nearFlights poll`);
      this.nearFlights.add(flight.fa_flight_id);
    } else {
      const sleepMs = (to_waypoint - 20) / groundspeedMph * 3600 * 1000;
      this.logger.debug(`[SCHEDULE] ${flight.fa_flight_id} — FAR (${to_waypoint.toFixed(2)} mi), sleeping ${(sleepMs/60000).toFixed(1)} min`);
      setTimeout(() => this.wakeUpFlight(flight.fa_flight_id), sleepMs);
    }
  }

  async wakeUpFlight(fa_flight_id: string) {
    if (!this.flights.find(f => f.fa_flight_id === fa_flight_id)) {
      this.logger.debug(`[WAKEUP] ${fa_flight_id} — no longer in flights list, ignoring`);
      return;
    }
    this.logger.debug(`[WAKEUP] ${fa_flight_id} — woke up, adding to nearFlights`);
    this.nearFlights.add(fa_flight_id);
    await this.checkFlight(fa_flight_id);
  }

  async pollNearFlights() {
    for (const fa_flight_id of [...this.nearFlights]) {
      if (!this.flights.find(f => f.fa_flight_id === fa_flight_id)) {
        this.nearFlights.delete(fa_flight_id);
        continue;
      }
      await this.checkFlight(fa_flight_id);
    }
  }

  async checkFlight(fa_flight_id: string) {
    const flight = this.flights.find(f => f.fa_flight_id === fa_flight_id);
    if (!flight) return;
    const position = await this.aeroAPIservice.getFlightPosition(fa_flight_id);
    if (!position?.last_position) return;
    const to_waypoint = flight.calcDistance(Airport.finalWP, position.last_position);
    const groundspeedMph = position.last_position.groundspeed * 1.15078;

    this.logger.debug(`[CHECK] ${fa_flight_id} — to_waypoint=${to_waypoint.toFixed(2)} mi, groundspeed=${groundspeedMph.toFixed(0)} mph`);

    if (to_waypoint > 20) {
      const sleepMs = (to_waypoint - 20) / groundspeedMph * 3600 * 1000;
      this.logger.debug(`[CHECK] ${fa_flight_id} — moved FAR (>20 mi), removing from nearFlights, sleeping ${(sleepMs/60000).toFixed(1)} min`);
      this.nearFlights.delete(fa_flight_id);
      setTimeout(() => this.wakeUpFlight(fa_flight_id), sleepMs);
    } else if (to_waypoint < 4) {
      const duration = Math.round(8 / groundspeedMph * 3600);
      this.logger.debug(`[CHECK] ${fa_flight_id} — IN RANGE (<4 mi), triggering animation (duration=${duration}s)`);
      this.triggerPlane(duration, to_waypoint);
    } else {
      this.logger.debug(`[CHECK] ${fa_flight_id} — still near (4–20 mi), staying in nearFlights`);
    }
  }

  easterEggHunt() {
    console.log(this.easterEggCount);
    this.easterEggCount++;
    if (this.easterEggCount >= 7) {
      this.easterEgg = !this.easterEgg;
      this.easterEggCount = 0;
    }
  }

  openFlightRadar24(flight: Flight){
    if (!this.authService.isAuthenticated()) {
      this.snackBar.open('This is demo mode — data is not live.', undefined, { duration: 3000, verticalPosition: 'top', panelClass: 'demo-toast' });
      return;
    }
    let url = "https://www.flightradar24.com/" + flight.getFlight();
    window.open(url, "_blank", "noreferrer");
  }
}
