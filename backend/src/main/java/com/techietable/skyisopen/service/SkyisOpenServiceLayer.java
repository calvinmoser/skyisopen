package com.techietable.skyisopen.service;

import com.techietable.skyisopen.dto.Flight;
import com.techietable.skyisopen.dao.AeroAPIDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class SkyisOpenServiceLayer {

    @Autowired
    AeroAPIDao dao;

    public ArrayList<Flight> scheduledFlights(Integer maxPages) {
        UUID requestId = dao.requestRequestId(maxPages);

        Map<String, Object> params = new HashMap<>();
        params.put("max_pages", maxPages);
        System.out.println(new Date() + " Retrieving scheduled flights with max pages = " + maxPages);
        return dao.scheduledArrivals(requestId, params);
    }


    public Flight flightPosition(String id) {
        UUID requestId = dao.requestRequestId(1);

        System.out.println(new Date() + " Retrieving position for flight with id " + id);
        Flight flight = dao.flightPosition(requestId, id);

        if (flight == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Flight " + id + " not found.");
        }

        return flight;
    }
}