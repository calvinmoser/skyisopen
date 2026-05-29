package com.techietable.skyisopen.collector;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.techietable.skyisopen.dto.Flight;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class DataCollector {

    static final String BASE_URL = "https://aeroapi.flightaware.com/aeroapi";
    static final String ARRIVALS_URL = BASE_URL + "/airports/KIND/flights/arrivals?start={start}&end={end}&max_pages={max_pages}";
    static final String TRACK_URL = BASE_URL + "/flights/{id}/track";

    static final int LOOKBACK_HOURS = 24;
    static final int WINDOW_HOURS = 4;
    static final int MAX_PAGES = 10;
    static final int RATE_LIMIT_MS = 8_000;
    static final int PAGINATION_WAIT_MS = 60_000;

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

        Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES).minus(LOOKBACK_HOURS, ChronoUnit.HOURS);
        Instant end   = start.plus(WINDOW_HOURS, ChronoUnit.HOURS);

        System.out.println("Fetching arrivals from " + start + " to " + end + "...");
        List<Flight> arrivals = fetchAllArrivals(start.toString(), end.toString());
        System.out.println("Fetched " + arrivals.size() + " arrivals total. Waiting 60s before fetching tracks...");
        Thread.sleep(60_000);

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

    static List<Flight> fetchAllArrivals(String start, String end) throws InterruptedException {
        List<Flight> all = new ArrayList<>();

        Arrivals page = fetchArrivals(start, end);
        if (page != null) all.addAll(page.arrivals);

        while (page != null && page.links != null && page.links.next != null) {
            System.out.println("More pages available (" + all.size() + " arrivals so far), waiting 60s...");
            Thread.sleep(PAGINATION_WAIT_MS);
            page = fetchArrivalsNext(page.links.next);
            if (page != null) all.addAll(page.arrivals);
        }

        return all;
    }

    static Arrivals fetchArrivals(String start, String end) {
        Map<String, Object> params = Map.of("start", start, "end", end, "max_pages", MAX_PAGES);
        ResponseEntity<Arrivals> response = restTemplate.exchange(ARRIVALS_URL, HttpMethod.GET, entity, Arrivals.class, params);
        return response.getBody();
    }

    static Arrivals fetchArrivalsNext(String nextPath) {
        ResponseEntity<Arrivals> response = restTemplate.exchange(BASE_URL + nextPath, HttpMethod.GET, entity, Arrivals.class);
        return response.getBody();
    }

    static FlightTrack fetchTrack(String faFlightId) {
        ResponseEntity<FlightTrack> response = restTemplate.exchange(TRACK_URL, HttpMethod.GET, entity, FlightTrack.class, Map.of("id", faFlightId));
        return response.getBody();
    }
}
