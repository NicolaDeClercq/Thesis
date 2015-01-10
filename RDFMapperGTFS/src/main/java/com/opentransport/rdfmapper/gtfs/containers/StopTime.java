package com.opentransport.rdfmapper.gtfs.containers;

/**
 *
 * @author Nicola De Clercq
 */
public class StopTime {
    
    private Stop stop;
    private String arrivalTime;
    private String departureTime;

    public StopTime(Stop stop, String arrivalTime, String departureTime) {
        this.stop = stop;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
    }

    public Stop getStop() {
        return stop;
    }

    public void setStop(Stop stop) {
        this.stop = stop;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    @Override
    public String toString() {
        return stop + " (" + arrivalTime + " - " + departureTime + ")";
    }
    
}