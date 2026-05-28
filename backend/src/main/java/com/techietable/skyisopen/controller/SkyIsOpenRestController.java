package com.techietable.skyisopen.controller;

import com.techietable.skyisopen.dto.Flight;
import com.techietable.skyisopen.service.DemoService;
import com.techietable.skyisopen.service.SkyisOpenServiceLayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/aero")
public class SkyIsOpenRestController {

    @Autowired
    SkyisOpenServiceLayer service;

    @Autowired
    DemoService demoService;

    @GetMapping("/scheduled_arrivals")
    public synchronized List<Flight> getScheduledFlights(Authentication auth, @RequestParam(required = false) Integer maxPages) {

        if (maxPages == null)
            maxPages = 1;
        else if (maxPages < 1 || maxPages > 10)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "max_pages must be between 1 and 10 inclusively");

        if (auth == null || !auth.isAuthenticated()) return demoService.getArrivals(maxPages);

        return service.scheduledFlights(maxPages);
    }

    @GetMapping("/flights/search")
    public synchronized ArrayList<Flight> searchAreaForPlanes (
        @RequestParam(required = false) Integer maxPages)
    {
        if (maxPages == null)
            maxPages = 1;
        else if (maxPages < 1 || maxPages > 10)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "max_pages must be between 1 and 10 inclusively");

        return service.searchAreaForPlanes(maxPages);
    }

    @GetMapping("/flights/{id}/position")
    public synchronized Flight getFlightPosition(Authentication auth, @PathVariable String id) {

        if (auth == null || !auth.isAuthenticated()) return demoService.getPosition(id);

        if (id == null || id.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id not specified");

        return service.flightPosition(id);
    }
}
