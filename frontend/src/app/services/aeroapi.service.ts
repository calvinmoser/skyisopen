import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Flight, Position } from '../model/flight';

@Injectable({
  providedIn: 'root'
})
export class AeroAPIService{

  constructor(private httpClient: HttpClient) {}


  getScheduledArrivals(maxPages: number): Observable<Flight[]> {
    const options = maxPages ? { params: new HttpParams().set('maxPages', maxPages) } : {};
    return this.httpClient.get<Flight[]>("/scheduled_arrivals", options)
      .pipe(
        map(flights => flights.map(flight => new Flight(flight)))
      );
  }

  getFlightPosition(fa_flight_id: string): Promise<Flight | undefined> {
    var flight_position_url = "/flights/" + fa_flight_id + "/position";
    return this.httpClient.get<Flight>(flight_position_url)
      .pipe(
        map(last_position => new Flight(last_position))
      ).toPromise();
  }

}
