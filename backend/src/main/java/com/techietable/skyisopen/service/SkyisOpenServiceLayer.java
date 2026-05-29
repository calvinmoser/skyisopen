package com.techietable.skyisopen.service;

import com.techietable.skyisopen.controller.SkyIsExceptional;
import com.techietable.skyisopen.dto.Flight;
import com.techietable.skyisopen.dao.AeroAPIDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class SkyisOpenServiceLayer {

    @Autowired
    AeroAPIDao dao;

    public ArrayList<Flight> scheduledFlights(Integer maxPages) {
        UUID requestId = dao.requestRequestId(maxPages);

        Map<String, Object> params = new HashMap<>();
        params.put("max_pages", maxPages);
        log.debug("scheduledFlights: maxPages={}", maxPages);
        return dao.scheduledArrivals(requestId, params);
    }

    public ArrayList<Flight> searchAreaForPlanes(Integer maxPages) {
        UUID requestId = dao.requestRequestId(maxPages);

        Map<String, Object> params = new HashMap<>();
        params.put("max_pages", maxPages);
        params.put("query", this.generateQuery());
        log.debug("searchAreaForPlanes: query={}", params.get("query"));
        return dao.searchAreaForPlanes(requestId, params);
    }

    private String generateQuery() {
        String queryString = new StringBuilder()
            .append(" -aboveAltitude ").append(1)
            .append(" -destination ").append("KIND")
            .toString();

        return queryString;
    }

    public Flight flightPosition(String id) {
        UUID requestId = dao.requestRequestId(1);

        log.debug("flightPosition: id={}", id);
        Flight flight = dao.flightPosition(requestId, id);

        if (flight == null) {
            log.error("flightPosition: null flight returned for id={}", id);
            throw new SkyIsExceptional(HttpStatus.INTERNAL_SERVER_ERROR, "Flight " + id + " not found.");
        }

        return flight;
    }
}