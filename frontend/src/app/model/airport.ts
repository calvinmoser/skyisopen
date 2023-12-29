import { Position } from '../model/flight';

export class Airport {
  static readonly position: Position = {
    altitude: 0,
    altitude_change: "",
    fa_flight_id: "",
    groundspeed: 0,
    heading: -1,
    latitude: 39.7205,
    longitude: -86.2937,
    timestamp: new Date(),
    update_type: ""
  }
  static readonly finalWP: Position = {
    altitude: 0,
    altitude_change: "",
    fa_flight_id: "",
    groundspeed: 0,
    heading: -1,
    latitude: 39.7868,
    longitude: -86.18412,
    timestamp: new Date(),
    update_type: ""
  }
}
