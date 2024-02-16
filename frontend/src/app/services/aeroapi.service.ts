import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Flight, Position } from '../model/flight';

@Injectable({
  providedIn: 'root'
})
export class AeroAPIService{

  constructor(private httpClient: HttpClient) {}

  getScheduledArrivals(maxPages: number): Observable<Flight[]> {
    const params = maxPages ? new HttpParams().set('maxPages', maxPages) : {};
    const options = { params: params };

    return this.httpClient.get<Flight[]>("/aero/scheduled_arrivals", options)
      .pipe(
        map(flights => flights.map(flight => new Flight(flight)))
      );
  }

  getFlightPosition(fa_flight_id: string): Promise<Flight | undefined> {
    var flight_position_url = "/aero/flights/" + fa_flight_id + "/position";

    return this.httpClient.get<Flight>(flight_position_url)
      .pipe(
        map(last_position => new Flight(last_position))
      ).toPromise();
  }

}
