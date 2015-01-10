package com.opentransport.rdfmapper.gtfs.containers;

import java.util.List;

/**
 *
 * @author Nicola De Clercq
 */
public class MapperInput {
    
    private List<Stop> stops;
    private List<Trip> trips;
    private String agency;

    public MapperInput(List<Stop> stops, List<Trip> trips, String agency) {
        this.stops = stops;
        this.trips = trips;
        this.agency = agency;
    }

    public List<Stop> getStops() {
        return stops;
    }

    public void setStops(List<Stop> stops) {
        this.stops = stops;
    }

    public List<Trip> getTrips() {
        return trips;
    }

    public void setTrips(List<Trip> trips) {
        this.trips = trips;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }
    
}