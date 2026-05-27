package com.techietable.skyisopen.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techietable.skyisopen.dto.Flight;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Service
public class DemoService {

    private List<Flight> arrivals = new ArrayList<>();
    private Map<String, List<Flight>> flightHistory = new LinkedHashMap<>();
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
            flightHistory = mapper.readValue(new File("backend/data/flight-history.json"),
                new TypeReference<Map<String, List<Flight>>>() {});
            System.out.println("DemoService: Loaded " + flightHistory.size() + " flight histories.");
        } catch (Exception e) {
            System.out.println("DemoService: Failed to load flight history - " + e.getMessage());
        }
        loopStartTime = System.currentTimeMillis();
    }

    public List<Flight> getArrivals() {
        return arrivals;
    }

    public Flight getPosition(String faFlightId) {
        List<Flight> history = flightHistory.get(faFlightId);
        if (history == null || history.isEmpty()) return null;

        List<Flight> snapshots = new ArrayList<>();
        for (Flight s : history) {
            if (s.last_position != null && s.last_position.latitude != 0 && s.last_position.longitude != 0) {
                snapshots.add(s);
            }
        }
        if (snapshots.isEmpty()) return null;

        snapshots.sort(Comparator.comparingLong(s -> s.last_position.timestamp != null ? s.last_position.timestamp.getTime() : 0));

        long firstTs = snapshots.get(0).last_position.timestamp.getTime();
        long lastTs = snapshots.get(snapshots.size() - 1).last_position.timestamp.getTime();
        long loopDuration = lastTs - firstTs;
        if (loopDuration <= 0) return snapshots.get(0);

        long elapsed = (System.currentTimeMillis() - loopStartTime) % loopDuration;
        long virtualTs = firstTs + elapsed;

        Flight prev = snapshots.get(0);
        Flight next = snapshots.get(snapshots.size() - 1);
        for (int i = 0; i < snapshots.size() - 1; i++) {
            long t0 = snapshots.get(i).last_position.timestamp.getTime();
            long t1 = snapshots.get(i + 1).last_position.timestamp.getTime();
            if (virtualTs >= t0 && virtualTs <= t1) {
                prev = snapshots.get(i);
                next = snapshots.get(i + 1);
                break;
            }
        }

        return deadReckon(prev, next, virtualTs);
    }

    private Flight deadReckon(Flight prev, Flight next, long virtualTs) {
        long t0 = prev.last_position.timestamp.getTime();
        long t1 = next.last_position.timestamp.getTime();
        double frac = t0 == t1 ? 0 : (double)(virtualTs - t0) / (t1 - t0);

        Flight result = new Flight();
        result.fa_flight_id = prev.fa_flight_id;
        result.operator = prev.operator;
        result.flight_number = prev.flight_number;
        result.last_position = new Flight.Position();
        result.last_position.latitude  = (float)(prev.last_position.latitude  + frac * (next.last_position.latitude  - prev.last_position.latitude));
        result.last_position.longitude = (float)(prev.last_position.longitude + frac * (next.last_position.longitude - prev.last_position.longitude));
        result.last_position.altitude  = prev.last_position.altitude;
        result.last_position.groundspeed = prev.last_position.groundspeed;
        result.last_position.heading   = prev.last_position.heading;
        result.last_position.timestamp = new Date(virtualTs);
        return result;
    }
}