package com.techietable.skyisopen.dto;

import java.util.ArrayList;
import java.util.Date;

public class Flight {
    public String ident;
    public String ident_icao;
    public String ident_iata;
    public String actual_runway_off;
    public String actual_runway_on;
    public String fa_flight_id;
    public String operator;
    public String operator_icao;
    public String operator_iata;
    public String flight_number;
    public String registration;
    public String atc_ident;
    public String inbound_fa_flight_id;
    public ArrayList<String> codeshares;
    public ArrayList<String> codeshares_iata;
    public boolean blocked;
    public boolean diverted;
    public boolean cancelled;
    public boolean position_only;
    public Airport origin;
    public Airport destination;
    public int departure_delay;
    public int arrival_delay;
    public int filed_ete;
    public int progress_percent;
    public String status;
    public String aircraft_type;
    public int route_distance;
    public int filed_airspeed;
    public int filed_altitude;
    public String route;
    public String baggage_claim;
    public int seats_cabin_business;
    public int seats_cabin_coach;
    public int seats_cabin_first;
    public String gate_origin;
    public String gate_destination;
    public String terminal_origin;
    public String terminal_destination;
    public ArrayList<Integer> waypoints;
    public Date first_position_time;
    public Position last_position;
    public ArrayList<Integer> bounding_box;
    public String ident_prefix;
    public String type;
    public Date scheduled_out;
    public Date estimated_out;
    public Date actual_out;
    public Date scheduled_off;
    public Date estimated_off;
    public Date actual_off;
    public Date scheduled_on;
    public Date estimated_on;
    public Date actual_on;
    public Date scheduled_in;
    public Date estimated_in;
    public Date actual_in;
    public boolean foresight_predictions_available;

    public static class Airport {
        public String code;
        public String code_icao;
        public String code_iata;
        public String code_lid;
        public String timezone;
        public String name;
        public String city;
        public String airport_info_url;
    }

    public static class Position{
        public String fa_flight_id;
        public int altitude;
        public String altitude_change;
        public int groundspeed;
        public int heading;
        public float latitude;
        public float longitude;
        public Date timestamp;
        public String update_type;
    }

}
