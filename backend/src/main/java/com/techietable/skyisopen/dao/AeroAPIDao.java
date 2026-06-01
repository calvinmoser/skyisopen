package com.techietable.skyisopen.dao;

import com.techietable.skyisopen.controller.SkyIsExceptional;
import com.techietable.skyisopen.controller.RequestLimitException;
import com.techietable.skyisopen.dto.Flight;
import com.techietable.skyisopen.dto.ScheduledArrivals;
import com.techietable.skyisopen.dto.SearchFlights;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.*;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.Duration;
import java.util.*;

@Slf4j
@Repository
@PropertySource("classpath:password.properties")
public class AeroAPIDao {
    final String ARRIVALS_URL = "https://aeroapi.flightaware.com/aeroapi/airports/IND/flights/scheduled_arrivals?max_pages={max_pages}";
    final String SEARCH_URL = "https://aeroapi.flightaware.com/aeroapi/flights/search?query={query}";
    final String FLIGHT_URL = "https://aeroapi.flightaware.com/aeroapi/flights/";

    private final HttpEntity<String> entity;

    private final Map<Instant, Integer> timestamps = new HashMap<>();
    private final Set<UUID> validRequests = new HashSet<>();

    private final RestTemplate restTemplate = new RestTemplate();

    public AeroAPIDao(@Value( "${aeroapi.password}") String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apikey", password);
        entity = new HttpEntity<>("body", headers);
    }

    public UUID requestRequestId(Integer numPages) {
        timestamps.entrySet().removeIf(t -> Duration.between(t.getKey(), Instant.now()).toSeconds() > 60);
        int pagesRetrieved = timestamps.values().stream().reduce(0, Integer::sum);
        log.debug("requestRequestId: pages requested in last minute={}", pagesRetrieved);
        if (pagesRetrieved + numPages > 10) {
            log.warn("requestRequestId: rate limit hit, pagesRetrieved={}, numRequested={}", pagesRetrieved, numPages);
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
            log.error("scheduledArrivals: invalid requestId={}", requestId);
            throw new SkyIsExceptional(HttpStatus.INTERNAL_SERVER_ERROR, "Internal request Id not valid.");
        }

        try {
            ResponseEntity<ScheduledArrivals> response = restTemplate.exchange(
                ARRIVALS_URL,
                HttpMethod.GET,
                entity,
                ScheduledArrivals.class,
                params
            );

            if (response.getBody() == null) {
                log.error("scheduledArrivals: empty response body, status={}", response.getStatusCode());
                throw new SkyIsExceptional(HttpStatus.INTERNAL_SERVER_ERROR, "Empty response body from AeroAPI.");
            }

            addPages(response.getBody().num_pages);
            log.debug("scheduledArrivals: received {} flights", response.getBody().scheduled_arrivals.size());

            return response.getBody().scheduled_arrivals;
        } catch (HttpClientErrorException e) {
            log.error("scheduledArrivals: AeroAPI returned status={}", e.getStatusCode(), e);
            throw new SkyIsExceptional(HttpStatus.valueOf(e.getStatusCode().value()), e.getMessage(), e);
        } finally {
            validRequests.remove(requestId);
        }
    }

    public ArrayList<Flight> searchAreaForPlanes(UUID requestId, Map<String, Object> params) {
        if (!validRequests.contains(requestId)) {
            log.error("searchAreaForPlanes: invalid requestId={}", requestId);
            throw new SkyIsExceptional(HttpStatus.INTERNAL_SERVER_ERROR, "Internal request Id not valid.");
        }
        try {
            ResponseEntity<SearchFlights> response = restTemplate.exchange(
                SEARCH_URL,
                HttpMethod.GET,
                entity,
                SearchFlights.class,
                params
            );

            SearchFlights searchFlights = response.getBody();

            if (searchFlights == null || searchFlights.flights == null) {
                log.error("searchAreaForPlanes: empty or null response body, status={}", response.getStatusCode());
                throw new SkyIsExceptional(HttpStatus.INTERNAL_SERVER_ERROR, "Empty response body from AeroAPI.");
            }

            addPages(searchFlights.num_pages);
            log.debug("searchAreaForPlanes: received {} flights", searchFlights.flights.size());

            return searchFlights.flights;
        } catch (HttpClientErrorException e) {
            log.error("searchAreaForPlanes: AeroAPI returned status={}", e.getStatusCode(), e);
            throw new SkyIsExceptional(HttpStatus.valueOf(e.getStatusCode().value()), e.getMessage(), e);
        }
    }

    public Flight flightPosition(UUID requestId, String id) {
        if (!validRequests.contains(requestId)) {
            log.error("flightPosition: invalid requestId={}", requestId);
            throw new SkyIsExceptional(HttpStatus.INTERNAL_SERVER_ERROR, "Internal request Id not valid.");
        }

        String url = FLIGHT_URL + id + "/position";

        try {
            ResponseEntity<Flight> response = restTemplate.exchange(url, HttpMethod.GET, entity, Flight.class);

            if (response.getBody() == null) {
                log.error("flightPosition: empty response body, id={}, status={}", id, response.getStatusCode());
                throw new SkyIsExceptional(HttpStatus.INTERNAL_SERVER_ERROR, "Empty response body from AeroAPI.");
            }

            addPages(1);
            log.debug("flightPosition: received position for id={}", id);

            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("flightPosition: AeroAPI returned status={}, id={}", e.getStatusCode(), id, e);
            throw new SkyIsExceptional(HttpStatus.valueOf(e.getStatusCode().value()), e.getMessage(), e);
        } finally {
            validRequests.remove(requestId);
        }
    }
}
