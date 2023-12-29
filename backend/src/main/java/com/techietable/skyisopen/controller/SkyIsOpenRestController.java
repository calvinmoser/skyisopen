package com.techietable.skyisopen.controller;

import com.techietable.skyisopen.dto.Flight;
import com.techietable.skyisopen.dto.ScheduledArrivals;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@RestController
@CrossOrigin()
@PropertySource("classpath:password.properties")
public class SkyIsOpenRestController {
    private final HttpEntity<String> entity;
    final String ARRIVALS_URL = "https://aeroapi.flightaware.com/aeroapi/airports/IND/flights/scheduled_arrivals?max_pages={max_pages}";
    final String FLIGHT_URL = "https://aeroapi.flightaware.com/aeroapi/flights/";

    public SkyIsOpenRestController(@Value( "${aeroapi.password}") String password) {
        // Password comes from password.properties
        // Header for access to AeroAPI
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apikey", password);
        entity = new HttpEntity<>("body", headers);
    }

    @GetMapping("/scheduled_arrivals")
    public synchronized ArrayList<Flight> getScheduledFlights(@RequestParam(required = false) Integer maxPages) {
        timestamps.removeIf(t -> Duration.between(t, Instant.now()).toSeconds() > 60);
        if (timestamps.size() >= 10) {
            System.out.println("< < < < < < < ERROR: Too many requests in past minute. > > > > > > > > > >");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR: Too many requests in past minute.");
        }
        timestamps.add(Instant.now());

        if (maxPages == null) maxPages = 1;
        else if (maxPages < 1 || maxPages > 10)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "max_pages can be between 1 and 10 inclusively");

        Map<String, Object> params = new HashMap<>();
        params.put("max_pages", maxPages);

        try {
            ResponseEntity<ScheduledArrivals> response = new RestTemplate().exchange(
                    ARRIVALS_URL,
                    HttpMethod.GET,
                    entity,
                    ScheduledArrivals.class,
                    params
            );
            if (response.getBody() == null)
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR: body missing in response. Status Code: " + response.getStatusCode());
            ArrayList<Flight> flights = response.getBody().scheduled_arrivals;
            System.out.println("New data retrieved: < < < " + new Date() + "> > >");
            return flights;
        } catch (HttpClientErrorException e) {
            // Pass back the response with slick new Spring Boot feature
            throw new ResponseStatusException(e.getStatusCode(), e.getMessage());
        }

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
    public synchronized Flight getFlightPosition(@PathVariable String id) {
        timestamps.removeIf(t -> Duration.between(t, Instant.now()).toSeconds() > 60);
        if (timestamps.size() > 10) {
            System.out.println("< < < < < < < ERROR: Too many requests in past minute. > > > > > > > > > >");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR: Too many requests in past minute.");
        }
        timestamps.add(Instant.now());

        System.out.println("Retrieving Position for: " + id + " (last minute: " + timestamps.size() + ")");
        String url = FLIGHT_URL + id + "/position";

        try {
            ResponseEntity<Flight> response = new RestTemplate().exchange(url, HttpMethod.GET, entity, Flight.class);
            if (response.getBody() == null)
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR: body missing in response. Status Code: " + response.getStatusCode());
            Flight flight = response.getBody();
            return flight;
        } catch (HttpClientErrorException e) {
            // Send response to frontend
            throw new ResponseStatusException(e.getStatusCode(), e.getMessage());
        }
    }
}
