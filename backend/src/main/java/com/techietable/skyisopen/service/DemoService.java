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

    static final long T0 = 1779816300000L; // 2026-05-26 12:05:00
    static final long LOOP_DURATION = 3 * 60 * 60 * 1000L; // 3 hours

    private List<Flight> arrivals = new ArrayList<>();
    private Map<String, FlightTrack> trackHistory = new LinkedHashMap<>();
    private Map<String, long[]> trackBounds = new LinkedHashMap<>(); // fa_flight_id -> [firstTs, lastTs]
    private Map<String, Long> originalEstimatedOn = new LinkedHashMap<>();

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

        for (Map.Entry<String, FlightTrack> entry : trackHistory.entrySet()) {
            List<FlightTrack.Position> positions = entry.getValue().positions;
            if (positions == null || positions.isEmpty()) continue;
            long first = positions.stream().filter(p -> p.timestamp != null)
                .mapToLong(p -> p.timestamp.getTime()).min().orElse(T0);
            long last = positions.stream().filter(p -> p.timestamp != null)
                .mapToLong(p -> p.timestamp.getTime()).max().orElse(T0);
            trackBounds.put(entry.getKey(), new long[]{first, last});
        }
        for (Flight flight : arrivals) {
            if (flight.estimated_on != null)
                originalEstimatedOn.put(flight.fa_flight_id, flight.estimated_on.getTime());
        }
        System.out.println("DemoService: Loop duration = " + LOOP_DURATION / 60000 + " min");
    }

    private long virtualNow() {
        return T0 + (System.currentTimeMillis() - T0) % LOOP_DURATION;
    }

    public List<Flight> getArrivals() {
        long vNow = virtualNow();
        for (Flight flight : arrivals) {
            long[] bounds = trackBounds.get(flight.fa_flight_id);
            if (bounds == null) continue;
            long lastTs = bounds[1];

            if (lastTs <= vNow) {
                flight.progress_percent = 0;
            } else {
                Date offDate = flight.actual_off != null ? flight.actual_off : flight.estimated_off;
                Long on = originalEstimatedOn.get(flight.fa_flight_id);
                if (offDate != null && on != null) {
                    long off = offDate.getTime();
                    if (on > off) flight.progress_percent = Math.max(0, (int)((vNow - off) * 100 / (on - off)));
                }
            }


            // map virtual arrival to real wall-clock time so the UI shows a meaningful estimated time
            long virtualArrival = lastTs <= vNow ? lastTs + LOOP_DURATION : lastTs;
            flight.estimated_on = new Date(System.currentTimeMillis() + (virtualArrival - vNow));
        }
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

        long vNow = virtualNow();
        if (vNow < firstTs) return buildFlight(faFlightId, positions.get(0));
        if (vNow > lastTs) return buildFlight(faFlightId, positions.get(positions.size() - 1));

        FlightTrack.Position prev = positions.get(0);
        FlightTrack.Position next = positions.get(positions.size() - 1);
        for (int i = 0; i < positions.size() - 1; i++) {
            long t0 = positions.get(i).timestamp.getTime();
            long t1 = positions.get(i + 1).timestamp.getTime();
            if (vNow >= t0 && vNow <= t1) {
                prev = positions.get(i);
                next = positions.get(i + 1);
                break;
            }
        }

        return deadReckon(faFlightId, prev, next, vNow);
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
