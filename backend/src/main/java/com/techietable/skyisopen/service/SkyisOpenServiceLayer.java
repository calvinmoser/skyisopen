package com.techietable.skyisopen.service;

import com.techietable.skyisopen.dto.Flight;
import com.techietable.skyisopen.dao.AeroAPIDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class SkyisOpenServiceLayer {
    final double BOX_SIZE =  4 * (1f/69);  // miles

    @Autowired
    AeroAPIDao dao;

    public ArrayList<Flight> scheduledFlights(Integer maxPages) {
        UUID requestId = dao.requestRequestId(maxPages);

        Map<String, Object> params = new HashMap<>();
        params.put("max_pages", maxPages);
        System.out.println(new Date() + " Retrieving scheduled flights with max pages = " + maxPages);
        return dao.scheduledArrivals(requestId, params);
    }

    public ArrayList<Flight> searchAreaForPlanes(Integer maxPages, Float latitude, Float longitude) {
        UUID requestId = dao.requestRequestId(maxPages);

        Map<String, Object> params = new HashMap<>();
        params.put("query", this.generateQuery(latitude, longitude));
        System.out.println(new Date() + " query: " + params.get("query"));
        return dao.searchAreaForPlanes(requestId, params);
    }

    private String generateQuery(Float latitude, Float longitude) {
        BigDecimal bottomLat = new BigDecimal(latitude - (BOX_SIZE / 2)).setScale(4, RoundingMode.HALF_EVEN);
        BigDecimal bottomLong = new BigDecimal(longitude - (BOX_SIZE / 2)).setScale(4, RoundingMode.HALF_EVEN);
        BigDecimal topLat = new BigDecimal(latitude + (BOX_SIZE / 2)).setScale(4, RoundingMode.HALF_EVEN);
        BigDecimal topLong = new BigDecimal(longitude + (BOX_SIZE / 2)).setScale(4, RoundingMode.HALF_EVEN);

        String queryString = new StringBuilder("-latlong \"")
                .append(bottomLat).append(" ")
                .append(bottomLong).append(" ")
                .append(topLat).append(" ")
                .append(topLong).append("\"").append(" ")
                .append(" -belowAltitude ").append(100)
                .append(" -aboveAltitude ").append(1)
//                .append(" -destination ").append("KIND")
                .toString();

        return queryString;
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