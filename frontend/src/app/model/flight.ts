export class Flight {
  ident: string = "";
  ident_icao: string = "";
  ident_iata: string = "";
  actual_runway_off: string = "";
  actual_runway_on: any = null;
  fa_flight_id: string = "";
  operator: string = "";
  operator_icao: string = "";
  operator_iata: string = "";
  flight_number: string = "";
  registration: string = "";
  atc_ident: any = null;
  inbound_fa_flight_id: string = "";
  codeshares: any[] = [];
  codeshares_iata: any[] = [];
  blocked: boolean = false;
  diverted: boolean = false;
  cancelled: boolean = false;
  position_only: boolean = false;
  origin: Airport = {code: "", code_icao: "", code_iata: "", code_lid: "", timezone: "", name: "", city: "", airport_info_url: ""};
  destination: Airport = {code: "", code_icao: "", code_iata: "", code_lid: "", timezone: "", name: "", city: "", airport_info_url: ""};
  departure_delay: number = 0;
  arrival_delay: number = 0;
  filed_ete: number = 0;
  scheduled_out: string = "";
  estimated_out: string = "";
  actual_out: any = null;
  scheduled_off: string = "";
  estimated_off: string = "";
  actual_off: string = "";
  scheduled_on: string = "";
  estimated_on: string = "";
  actual_on: any = null;
  scheduled_in: any = null;
  estimated_in: any = null;
  actual_in: any = null;
  progress_percent: number = 0;
  status: string = "";
  aircraft_type: string = "";
  route_distance: number = 0;
  filed_airspeed: number = 0;
  filed_altitude: number = 0;
  route: string = "";
  baggage_claim: any = null;
  seats_cabin_business: any = null;
  seats_cabin_coach: any = null;
  seats_cabin_first: any = null;
  gate_origin: any = null;
  gate_destination: any = null;
  terminal_origin: any = null;
  terminal_destination: any = null;
  type: string = "";
  last_position: Position = {
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
  // extra
  to_airport: string = "";
  to_waypoint: string = "";
  estimated: string = "";
  updated: string = "";
  next_update: string = "";
  color: string = "";
  inside: boolean = false;
  angle: string = "";
  added: string = "";
  log: string = "";

  constructor(data: Object|Flight) {
    Object.assign(this,data);
  }
}

export interface Airport {
  code: string
  code_icao: string
  code_iata: string
  code_lid: string
  timezone: string
  name: string
  city: string
  airport_info_url: string
}

export interface Position {
  altitude: number,
  altitude_change: string,
  fa_flight_id: string,
  groundspeed: number,
  heading: number,
  latitude: number,
  longitude: number,
  timestamp: Date,
  update_type: string
}

