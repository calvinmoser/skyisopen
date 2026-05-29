import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Flight, Position } from '../model/flight';
import { NGXLogger } from 'ngx-logger';

@Injectable({
  providedIn: 'root'
})
export class AeroAPIService{

  constructor(private httpClient: HttpClient, private logger: NGXLogger) {}

  getScheduledArrivals(maxPages: number): Observable<Flight[]> {
    const params = maxPages ? new HttpParams().set('maxPages', maxPages) : {};
    const options = { params: params };

    this.logger.debug(`getScheduledArrivals: requesting ${maxPages} page(s)`);

    return this.httpClient.get<Flight[]>("/aero/scheduled_arrivals", options)
      .pipe(
        map(flights => flights.map(flight => new Flight(flight))),
        tap(flights => this.logger.debug(`getScheduledArrivals: received ${flights.length} flights`))
      );
  }

  getFlightPosition(fa_flight_id: string): Promise<Flight | undefined> {
    var flight_position_url = "/aero/flights/" + fa_flight_id + "/position";

    this.logger.debug(`getFlightPosition: requesting position for ${fa_flight_id}`);

    return this.httpClient.get<Flight>(flight_position_url)
      .pipe(
        map(last_position => new Flight(last_position)),
        tap(flight => this.logger.debug(`getFlightPosition: received position for ${fa_flight_id}`))
      ).toPromise();
  }

}
