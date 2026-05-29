package com.techietable.skyisopen.controller;

import com.techietable.skyisopen.dto.Flight;
import com.techietable.skyisopen.service.DemoService;
import com.techietable.skyisopen.service.SkyisOpenServiceLayer;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/aero")
public class SkyIsOpenRestController {

    @Autowired
    SkyisOpenServiceLayer service;

    @Autowired
    DemoService demoService;

    @GetMapping("/scheduled_arrivals")
    public synchronized List<Flight> getScheduledFlights(Authentication auth, @RequestParam(required = false) Integer maxPages, HttpServletResponse response) {
        log.debug("getScheduledFlights: maxPages={}", maxPages);
        response.setHeader("Cache-Control", "no-store");

        if (maxPages == null)
            maxPages = 1;
        else if (maxPages < 1 || maxPages > 10)
            throw new SkyIsExceptional(HttpStatus.BAD_REQUEST, "max_pages must be between 1 and 10 inclusively");

        if (auth == null || !auth.isAuthenticated()) {
            log.debug("getScheduledFlights (DEMO): unauthenticated, routing to demo");
            List<Flight> demoFlights = demoService.getArrivals(maxPages);
            log.debug("getScheduledFlights (DEMO): returning {} demo flights", demoFlights.size());
            return demoFlights;
        }

        ArrayList<Flight> flights = service.scheduledFlights(maxPages);
        log.debug("getScheduledFlights: returning {} flights", flights.size());
        return flights;

    }

    @GetMapping("/flights/search")
    public synchronized ArrayList<Flight> searchAreaForPlanes(
        @RequestParam(required = false) Integer maxPages)
    {
        log.debug("searchAreaForPlanes: maxPages={}", maxPages);

        if (maxPages == null)
            maxPages = 1;
        else if (maxPages < 1 || maxPages > 10)
            throw new SkyIsExceptional(HttpStatus.BAD_REQUEST, "max_pages must be between 1 and 10 inclusively");

        ArrayList<Flight> flights = service.searchAreaForPlanes(maxPages);
        log.debug("searchAreaForPlanes: returning {} flights", flights.size());
        return flights;
    }

    @GetMapping("/flights/{id}/position")
    public synchronized Flight getFlightPosition(Authentication auth, @PathVariable String id) {
        log.debug("getFlightPosition: id={}", id);

        if (id == null || id.isEmpty())
            throw new SkyIsExceptional(HttpStatus.BAD_REQUEST, "id not specified");

        if (auth == null || !auth.isAuthenticated()) {
            log.debug("getFlightPosition (DEMO): unauthenticated, routing to demo");
            Flight demoFlight = demoService.getPosition(id);
            log.debug("getFlightPosition (DEMO): returning demo position for id={}, found={}", id, demoFlight != null);
            return demoFlight;
        }

        Flight flight = service.flightPosition(id);
        log.debug("getFlightPosition: returning position for id={}", id);
        return flight;
    }
}
