package com.opentransport.rdfrouteplanner.containers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Nicola De Clercq
 */
public class Station {
    
    private String id;
    private String name;
    private double latitude;
    private double longitude;
    private Calendar earliestArrivalTime;
    private Connection previous;
    private List<Connection> sameTripConnections;
    
    public Station() {}

    public Station(String id, String name, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        earliestArrivalTime = Calendar.getInstance();
        earliestArrivalTime.setTime(new Date(Long.MAX_VALUE - 1000000000));
        previous = null;
        sameTripConnections = new ArrayList<>();
    }
    
    public Station(Station s) {
        this.id = s.getId();
        this.name = s.getName();
        this.latitude = s.getLatitude();
        this.longitude = s.getLongitude();
        earliestArrivalTime = Calendar.getInstance();
        earliestArrivalTime.setTime(new Date(Long.MAX_VALUE - 1000000000));
        previous = null;
        sameTripConnections = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Calendar getEarliestArrivalTime() {
        return earliestArrivalTime;
    }

    public void setEarliestArrivalTime(Calendar earliestArrivalTime) {
        this.earliestArrivalTime = earliestArrivalTime;
    }

    public Connection getPrevious() {
        return previous;
    }

    public void setPrevious(Connection previous) {
        this.previous = previous;
    }

    public List<Connection> getSameTripConnections() {
        return sameTripConnections;
    }

    public void setSameTripConnections(List<Connection> sameTripConnections) {
        this.sameTripConnections = sameTripConnections;
    }
    
    @Override
    public String toString() {
        return name + " (" + longitude + ", " + latitude + ") [" + id + "]";
    }
    
}