package com.techietable.skyisopen.controller;

import com.techietable.skyisopen.dto.Flight;
import com.techietable.skyisopen.service.SkyisOpenServiceLayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/aero")
public class SkyIsOpenRestController {

    @Autowired
    SkyisOpenServiceLayer service;

    @GetMapping("/scheduled_arrivals")
    public synchronized ArrayList<Flight> getScheduledFlights(@RequestParam(required = false) Integer maxPages) {

        if (maxPages == null)
            maxPages = 1;
        else if (maxPages < 1 || maxPages > 10)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "max_pages must be between 1 and 10 inclusively");

        return service.scheduledFlights(maxPages);
    }

    @GetMapping("/flights/{id}/position")
    public synchronized Flight getFlightPosition(@PathVariable String id) {

        if (id == null || id == "")
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id not specified");

        return service.flightPosition(id);
    }
}
