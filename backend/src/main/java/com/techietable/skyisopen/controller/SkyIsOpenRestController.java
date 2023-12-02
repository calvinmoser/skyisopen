package com.techietable.skyisopen.controller;

import com.techietable.skyisopen.dto.Flight;
import com.techietable.skyisopen.dto.ScheduledArrivals;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@CrossOrigin()
@PropertySource("classpath:password.properties")
public class SkyIsOpenRestController {
    private final HttpEntity<String> entity;
    final String ARRIVALS_URL = "https://aeroapi.flightaware.com/aeroapi/airports/IND/flights/scheduled_arrivals";
    final String FLIGHT_URL = "https://aeroapi.flightaware.com/aeroapi/flights/";

    public SkyIsOpenRestController(@Value( "${aeroapi.password}") String password) {
        // Password comes from password.properties
        // Header for access to AeroAPI
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apikey", password);
        entity = new HttpEntity<>("body", headers);
    }

    @GetMapping()
    public synchronized ResponseEntity<ScheduledArrivals> getAllFlights() {
        timestamps.removeIf(t -> Duration.between(t, Instant.now()).toSeconds() > 60);
        if (timestamps.size() >= 10) {
            System.out.println("< < < < < < < ERROR: Too many requests in past minute. > > > > > > > > > >");
            return new ResponseEntity<ScheduledArrivals>(new ScheduledArrivals(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        timestamps.add(Instant.now());

        try {
            ResponseEntity<ScheduledArrivals> response = new RestTemplate().exchange(ARRIVALS_URL, HttpMethod.GET, entity, ScheduledArrivals.class);
            System.out.println("New data retrieved: < < < " + new Date() + "> > >");
            return response;
        } catch (HttpClientErrorException e) {
            // Pass back the response with slick new Spring Boot feature
            throw new ResponseStatusException(e.getStatusCode(), e.getMessage());
        }

    }

    List<Instant> timestamps = new ArrayList<>();

    @GetMapping("/flights/{id}/position")
    public synchronized ResponseEntity<Flight> getFlightPosition(@PathVariable String id) {
        timestamps.removeIf(t -> Duration.between(t, Instant.now()).toSeconds() > 60);
        if (timestamps.size() > 10) {
            System.out.println("< < < < < < < ERROR: Too many requests in past minute. > > > > > > > > > >");
            return new ResponseEntity<Flight>(new Flight(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        timestamps.add(Instant.now());

        System.out.println("Retrieving Position for: " + id + " (last minute: " + timestamps.size() + ")");
        String url = FLIGHT_URL + id + "/position";

        try {
            return new RestTemplate().exchange(url, HttpMethod.GET, entity, Flight.class);
        } catch (HttpClientErrorException e) {
            // Send response to frontend
            throw new ResponseStatusException(e.getStatusCode(), e.getMessage());
        }
    }
}
