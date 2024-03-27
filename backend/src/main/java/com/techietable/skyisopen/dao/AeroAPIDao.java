package com.techietable.skyisopen.dao;

import com.techietable.skyisopen.controller.RequestLimitException;
import com.techietable.skyisopen.dto.Flight;
import com.techietable.skyisopen.dto.ScheduledArrivals;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.*;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.Duration;
import java.util.*;

@Repository
@PropertySource("classpath:password.properties")
public class AeroAPIDao {
    final String ARRIVALS_URL = "https://aeroapi.flightaware.com/aeroapi/airports/IND/flights/scheduled_arrivals?max_pages={max_pages}";
    final String FLIGHT_URL = "https://aeroapi.flightaware.com/aeroapi/flights/";

    private final HttpEntity<String> entity;

    private final Map<Instant, Integer> timestamps = new HashMap<>();
    private final Set<UUID> validRequests = new HashSet<>();

    public AeroAPIDao(@Value( "${aeroapi.password}") String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apikey", password);
        entity = new HttpEntity<>("body", headers);
    }

    public UUID requestRequestId(Integer numPages) {
        timestamps.entrySet().removeIf(t -> Duration.between(t.getKey(), Instant.now()).toSeconds() > 60);
        int pagesRetrieved = timestamps.values().stream().reduce(0, Integer::sum);
        System.out.println(new Date() + " (Pages requested in last minute: " + pagesRetrieved + ")");
        if (pagesRetrieved + numPages > 10) {
            System.out.println(new Date() + " Too many requests in past minute.");
            // TODO: Wait instead of throwing exception
            throw new RequestLimitException("Too many requests in past minute.");
        }
        UUID requestId = UUID.randomUUID();
        validRequests.add(requestId);
        return requestId;
    }

    private void addPages(int numPages) {
        timestamps.put(Instant.now(), numPages);
    }

    public ArrayList<Flight> scheduledArrivals(UUID requestId, Map<String, Object> params) {
        if (!validRequests.contains(requestId)) {
            System.out.println(new Date() + " Request ID passed from service layer is not valid.");
            // TODO: This type of exception should not be thrown in dao
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal request Id not valid.");
        }

        try {
            ResponseEntity<ScheduledArrivals> response = new RestTemplate().exchange(
                    ARRIVALS_URL,
                    HttpMethod.GET,
                    entity,
                    ScheduledArrivals.class,
                    params
            );

            if (response.getBody() == null)
                // TODO: This type of exception should not be thrown in dao
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR: body missing in response. Status Code: " + response.getStatusCode());

            addPages(response.getBody().num_pages);

            return response.getBody().scheduled_arrivals;
        } catch (HttpClientErrorException e) {
            // TODO: This type of exception should not be thrown in dao
            throw new ResponseStatusException(e.getStatusCode(), e.getMessage());
        } finally {
            validRequests.remove(requestId);
        }
    }

    public Flight flightPosition(UUID requestId, String id) {
        if (!validRequests.contains(requestId)) {
            System.out.println(new Date() + " Request ID passed from service layer is not valid.");
            // TODO: This type of exception should not be thrown in dao
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal request Id not valid.");
        }

        String url = FLIGHT_URL + id + "/position";

        try {
            ResponseEntity<Flight> response = new RestTemplate().exchange(url, HttpMethod.GET, entity, Flight.class);

            if (response.getBody() == null)
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR: body missing in response. Status Code: " + response.getStatusCode());

            addPages(1);

            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(e.getStatusCode(), e.getMessage());
        } finally {
            validRequests.remove(requestId);
        }
    }
}
