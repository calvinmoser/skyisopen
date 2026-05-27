package com.techietable.skyisopen.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techietable.skyisopen.collector.FlightTrack;
import com.techietable.skyisopen.dto.Flight;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DemoService {

    private List<Flight> arrivals = new ArrayList<>();
    private Map<String, FlightTrack> trackHistory = new LinkedHashMap<>();
    private long loopStartTime;

    @PostConstruct
    public void load() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            arrivals = mapper.readValue(new File("backend/data/arrivals.json"),
                mapper.getTypeFactory().constructCollectionType(List.class, Flight.class));
            System.out.println("DemoService: Loaded " + arrivals.size() + " arrivals.");
        } catch (Exception e) {
            System.out.println("DemoService: Failed to load arrivals - " + e.getMessage());
        }
        try {
            trackHistory = mapper.readValue(new File("backend/data/track-history.json"),
                new TypeReference<Map<String, FlightTrack>>() {});
            System.out.println("DemoService: Loaded " + trackHistory.size() + " tracks.");
        } catch (Exception e) {
            System.out.println("DemoService: Failed to load track history - " + e.getMessage());
        }
        loopStartTime = System.currentTimeMillis();
    }

    public List<Flight> getArrivals() {
        return arrivals.stream()
            .sorted(Comparator.comparing(f -> f.estimated_on != null ? f.estimated_on : new Date(Long.MAX_VALUE)))
            .collect(Collectors.toList());
    }

    public Flight getPosition(String faFlightId) {
        FlightTrack track = trackHistory.get(faFlightId);
        if (track == null || track.positions == null || track.positions.isEmpty()) return null;

        List<FlightTrack.Position> positions = track.positions.stream()
            .filter(p -> p.latitude != 0 && p.longitude != 0)
            .sorted(Comparator.comparingLong(p -> p.timestamp != null ? p.timestamp.getTime() : 0))
            .collect(Collectors.toList());
        if (positions.isEmpty()) return null;

        long firstTs = positions.get(0).timestamp.getTime();
        long lastTs = positions.get(positions.size() - 1).timestamp.getTime();
        long loopDuration = lastTs - firstTs;
        if (loopDuration <= 0) return buildFlight(faFlightId, positions.get(0));

        long elapsed = (System.currentTimeMillis() - loopStartTime) % loopDuration;
        long virtualTs = firstTs + elapsed;

        FlightTrack.Position prev = positions.get(0);
        FlightTrack.Position next = positions.get(positions.size() - 1);
        for (int i = 0; i < positions.size() - 1; i++) {
            long t0 = positions.get(i).timestamp.getTime();
            long t1 = positions.get(i + 1).timestamp.getTime();
            if (virtualTs >= t0 && virtualTs <= t1) {
                prev = positions.get(i);
                next = positions.get(i + 1);
                break;
            }
        }

        return deadReckon(faFlightId, prev, next, virtualTs);
    }

    private Flight buildFlight(String faFlightId, FlightTrack.Position p) {
        Flight f = new Flight();
        f.fa_flight_id = faFlightId;
        f.last_position = new Flight.Position();
        f.last_position.latitude = p.latitude;
        f.last_position.longitude = p.longitude;
        f.last_position.altitude = p.altitude;
        f.last_position.groundspeed = p.groundspeed;
        f.last_position.heading = p.heading;
        f.last_position.timestamp = p.timestamp;
        return f;
    }

    private Flight deadReckon(String faFlightId, FlightTrack.Position prev, FlightTrack.Position next, long virtualTs) {
        long t0 = prev.timestamp.getTime();
        long t1 = next.timestamp.getTime();
        double frac = t0 == t1 ? 0 : (double)(virtualTs - t0) / (t1 - t0);

        Flight result = new Flight();
        result.fa_flight_id = faFlightId;
        result.last_position = new Flight.Position();
        result.last_position.latitude  = (float)(prev.latitude  + frac * (next.latitude  - prev.latitude));
        result.last_position.longitude = (float)(prev.longitude + frac * (next.longitude - prev.longitude));
        result.last_position.altitude  = prev.altitude;
        result.last_position.groundspeed = prev.groundspeed;
        result.last_position.heading   = prev.heading;
        result.last_position.timestamp = new Date(virtualTs);
        return result;
    }
}