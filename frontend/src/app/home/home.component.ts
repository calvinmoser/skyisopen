import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { AeroAPIService } from '../services/aeroapi.service';
import { Flight, Position } from '../model/flight';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [MatCheckboxModule, MatTableModule, CommonModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})

export class HomeComponent {

  dataSource = new MatTableDataSource<Flight>([]);
  flightMap: Map <String, Flight> = new Map<String, Flight>();
  totalCalls: number = 0;
  displayedColumns: string[] = [ "fa_flight_id", "flight", "aircraft_type", "estimated_on", "origin", "groundspeed",
    "altitude", "angle", "to_airport", "distance", "estimated", "next_update", "updated", /*"remove"*/ ];

  constructor(private aeroAPIservice: AeroAPIService) {}

  ngOnInit(): void {
    this.aeroAPIservice.getScheduledArrivals()
      .subscribe(flights => this.dataSource.data = flights);
  }

  removeFlight(flight: Flight) {};

}
