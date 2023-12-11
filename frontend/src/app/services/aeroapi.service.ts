import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { Flight, Position } from '../model/flight';

@Injectable({
  providedIn: 'root'
})
export class AeroAPIService{

  constructor(private httpClient: HttpClient) {}

  readonly scheduled_arrivals_url = "http://localhost:8080/scheduled_arrivals?maxPages=4";

  getScheduledArrivals(): Observable<Flight[]> {
    return this.httpClient.get<Flight[]>(this.scheduled_arrivals_url)
      .pipe(
        map(flights => flights.map(flight => new Flight(flight)))
      );
  }
}
