package com.techietable.skyisopen.collector;

import java.util.Date;
import java.util.List;

public class FlightTrack {
    public Integer actual_distance;
    public List<Position> positions;

    public static class Position {
        public int altitude;
        public String altitude_change;
        public int groundspeed;
        public Integer heading;
        public float latitude;
        public float longitude;
        public Date timestamp;
        public String update_type;
    }
}
