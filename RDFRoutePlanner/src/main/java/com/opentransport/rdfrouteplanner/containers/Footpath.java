package com.opentransport.rdfrouteplanner.containers;

import java.io.Serializable;

/**
 *
 * @author Nicola De Clercq
 */
public class Footpath implements Serializable {
    
    private String departureStationId;
    private String arrivalStationId;
    private int distance;
    private int duration;
    
    public Footpath() {}

    public Footpath(String departureStationId, String arrivalStationId, int distance, int duration) {
        this.departureStationId = departureStationId;
        this.arrivalStationId = arrivalStationId;
        this.distance = distance;
        this.duration = duration;
    }

    public String getDepartureStationId() {
        return departureStationId;
    }

    public void setDepartureStationId(String departureStationId) {
        this.departureStationId = departureStationId;
    }

    public String getArrivalStationId() {
        return arrivalStationId;
    }

    public void setArrivalStationId(String arrivalStationId) {
        this.arrivalStationId = arrivalStationId;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

}