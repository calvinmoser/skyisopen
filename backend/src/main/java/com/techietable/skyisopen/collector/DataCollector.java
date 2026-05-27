package com.techietable.skyisopen.collector;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.techietable.skyisopen.dto.Flight;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DataCollector {

    static final String BASE_URL = "https://aeroapi.flightaware.com/aeroapi";
    static final String ARRIVALS_URL = BASE_URL + "/history/airports/KIND/flights/arrivals?start={start}&end={end}&max_pages={max_pages}";
    static final String TRACK_URL = BASE_URL + "/history/flights/{id}/track";

    static final ZoneId ZONE = ZoneId.of("America/Indiana/Indianapolis");
    static final int START_HOUR = 10;
    static final int END_HOUR = 14;
    static final int MAX_PAGES = 10;
    static final int RATE_LIMIT_MS = 1100;

    static HttpEntity<String> entity;
    static RestTemplate restTemplate = new RestTemplate();
    static ObjectMapper mapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.load(new FileInputStream("backend/src/main/resources/password.properties"));

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apikey", props.getProperty("aeroapi.password"));
        entity = new HttpEntity<>("body", headers);

        LocalDate yesterday = LocalDate.now(ZONE).minusDays(1);
        String start = yesterday.atTime(START_HOUR, 0).atZone(ZONE).withZoneSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        String end   = yesterday.atTime(END_HOUR, 0).atZone(ZONE).withZoneSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

        System.out.println("Fetching arrivals for " + yesterday + " 10am-2pm ET...");
        List<Flight> arrivals = fetchArrivals(start, end);
        System.out.println("Fetched " + arrivals.size() + " arrivals.");

        Map<String, FlightTrack> tracks = new LinkedHashMap<>();
        for (Flight flight : arrivals) {
            System.out.println("Fetching track for " + flight.fa_flight_id + "...");
            FlightTrack track = fetchTrack(flight.fa_flight_id);
            if (track != null) tracks.put(flight.fa_flight_id, track);
            Thread.sleep(RATE_LIMIT_MS);
        }

        mapper.writeValue(new File("backend/data/arrivals.json"), arrivals);
        mapper.writeValue(new File("backend/data/flight-history.json"), tracks);
        System.out.println("Done. Wrote arrivals.json and flight-history.json.");
    }

    static List<Flight> fetchArrivals(String start, String end) {
        Map<String, Object> params = Map.of("start", start, "end", end, "max_pages", MAX_PAGES);
        ResponseEntity<Arrivals> response = restTemplate.exchange(ARRIVALS_URL, HttpMethod.GET, entity, Arrivals.class, params);
        return response.getBody() != null ? response.getBody().arrivals : List.of();
    }

    static FlightTrack fetchTrack(String faFlightId) {
        ResponseEntity<FlightTrack> response = restTemplate.exchange(TRACK_URL, HttpMethod.GET, entity, FlightTrack.class, Map.of("id", faFlightId));
        return response.getBody();
    }
}
