package com.opentransport.rdfrouteplanner.containers;

import java.util.Calendar;

/**
 *
 * @author Nicola De Clercq
 */
public class Connection {
    
    private String departureStationId;
    private Calendar scheduledDepartureTime;
    private int departureDelay;
    private Calendar actualDepartureTime;
    private String departurePlatform;
    private String arrivalStationId;
    private Calendar scheduledArrivalTime;
    private int arrivalDelay;
    private Calendar actualArrivalTime;
    private String arrivalPlatform;
    private String route;
    private String headsign;
    private int distance;
    
    public Connection() {}

    public Connection(String departureStationId, Calendar scheduledDepartureTime, int departureDelay, Calendar actualDepartureTime, String departurePlatform, String arrivalStationId, Calendar scheduledArrivalTime, int arrivalDelay, Calendar actualArrivalTime, String arrivalPlatform, String route, String headsign) {
        this.departureStationId = departureStationId;
        this.scheduledDepartureTime = scheduledDepartureTime;
        this.departureDelay = departureDelay;
        this.actualDepartureTime = actualDepartureTime;
        this.departurePlatform = departurePlatform;
        this.arrivalStationId = arrivalStationId;
        this.scheduledArrivalTime = scheduledArrivalTime;
        this.arrivalDelay = arrivalDelay;
        this.actualArrivalTime = actualArrivalTime;
        this.arrivalPlatform = arrivalPlatform;
        this.route = route;
        this.headsign = headsign;
    }

    /**
     * Constructor for footpath connections
     * @param f Footpath
     * @param departureTime Departure time of the footpath
     */
    public Connection(Footpath f, Calendar departureTime) {
        this.departureStationId = f.getDepartureStationId();
        this.scheduledDepartureTime = departureTime;
        this.departureDelay = 0;
        this.actualDepartureTime = departureTime;
        this.departurePlatform = "";
        this.arrivalStationId = f.getArrivalStationId();
        Calendar atf = (Calendar) departureTime.clone();
        atf.add(Calendar.MINUTE,f.getDuration());
        this.scheduledArrivalTime = atf;
        this.arrivalDelay = 0;
        this.actualArrivalTime = atf;
        this.arrivalPlatform = "";
        this.route = "WALK";
        this.headsign = "";
        this.distance = f.getDistance();
    }

    public String getDepartureStationId() {
        return departureStationId;
    }

    public void setDepartureStationId(String departureStationId) {
        this.departureStationId = departureStationId;
    }

    public Calendar getScheduledDepartureTime() {
        return scheduledDepartureTime;
    }

    public void setScheduledDepartureTime(Calendar scheduledDepartureTime) {
        this.scheduledDepartureTime = scheduledDepartureTime;
    }

    public int getDepartureDelay() {
        return departureDelay;
    }

    public void setDepartureDelay(int departureDelay) {
        this.departureDelay = departureDelay;
    }

    public Calendar getActualDepartureTime() {
        return actualDepartureTime;
    }

    public void setActualDepartureTime(Calendar actualDepartureTime) {
        this.actualDepartureTime = actualDepartureTime;
    }

    public String getDeparturePlatform() {
        return departurePlatform;
    }

    public void setDeparturePlatform(String departurePlatform) {
        this.departurePlatform = departurePlatform;
    }

    public String getArrivalStationId() {
        return arrivalStationId;
    }

    public void setArrivalStationId(String arrivalStationId) {
        this.arrivalStationId = arrivalStationId;
    }

    public Calendar getScheduledArrivalTime() {
        return scheduledArrivalTime;
    }

    public void setScheduledArrivalTime(Calendar scheduledArrivalTime) {
        this.scheduledArrivalTime = scheduledArrivalTime;
    }

    public int getArrivalDelay() {
        return arrivalDelay;
    }

    public void setArrivalDelay(int arrivalDelay) {
        this.arrivalDelay = arrivalDelay;
    }

    public Calendar getActualArrivalTime() {
        return actualArrivalTime;
    }

    public void setActualArrivalTime(Calendar actualArrivalTime) {
        this.actualArrivalTime = actualArrivalTime;
    }

    public String getArrivalPlatform() {
        return arrivalPlatform;
    }

    public void setArrivalPlatform(String arrivalPlatform) {
        this.arrivalPlatform = arrivalPlatform;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getHeadsign() {
        return headsign;
    }

    public void setHeadsign(String headsign) {
        this.headsign = headsign;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }
    
}