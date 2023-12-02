package com.techietable.skyisopen.dto;

import java.util.ArrayList;

public class ScheduledArrivals {
    public Links links;
    public int num_pages;
    public ArrayList<Flight> scheduled_arrivals;

    public static class Links{
        public String next;
    }
}
