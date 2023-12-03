import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { Flight, Position } from '../model/flight';

@Injectable({
  providedIn: 'root'
})
export class AeroAPIService{

  constructor(private httpClient: HttpClient) {}

  readonly scheduled_arrivals_url = "http://localhost:8080/scheduled_arrivals";

  getScheduledArrivals(): Observable<Flight[]> {
    return this.httpClient.get<Flight[]>(this.scheduled_arrivals_url)
  }
}