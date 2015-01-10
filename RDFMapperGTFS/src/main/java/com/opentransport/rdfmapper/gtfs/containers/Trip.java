package com.opentransport.rdfmapper.gtfs.containers;

import java.util.TreeMap;

/**
 *
 * @author Nicola De Clercq
 */
public class Trip {
        
    private String headsign;
    private Route route;
    private TreeMap<Integer,StopTime> stops;

    public Trip(String headsign, Route route) {
        this.headsign = headsign;
        this.route = route;
        this.stops = new TreeMap<>();
    }

    public String getHeadsign() {
        return headsign;
    }

    public void setHeadsign(String headsign) {
        this.headsign = headsign;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public TreeMap<Integer,StopTime> getStops() {
        return stops;
    }

    public void setStops(TreeMap<Integer,StopTime> stops) {
        this.stops = stops;
    }

    @Override
    public String toString() {
        return headsign + " (" + route + ")";
    }
    
}