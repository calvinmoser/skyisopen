import { Component } from '@angular/core';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatCheckboxChange } from '@angular/material/checkbox';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [MatCheckboxModule, MatTableModule, ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})

export class HomeComponent {

  dataSource = new MatTableDataSource<Flight>([]);
  flightMap: Map <String, Flight> = new Map<String, Flight>();
  totalCalls: number = 0;
  displayedColumns: string[] = ["flight"];

  constructor() {
    this.flightMap.set("1", new Flight().setId("DAL1234"));
    this.flightMap.set("2", new Flight().setId("AA1409"));
    this.flightMap.set("3", new Flight().setId("FDX2024"));
    this.updateDataSource();
  }

  updateDataSource() {
      var flights = Array.from(this.flightMap.values());
      flights = flights.sort((a,b) => a.next_update - b.next_update);
      this.dataSource.data = flights;
   }

}

export class Flight {
  id: string = "";
  next_update: number = 0;

  setId (id: string) {
    this.id = id;
    return this;
  }


}
