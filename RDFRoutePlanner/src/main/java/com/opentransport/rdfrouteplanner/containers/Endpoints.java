package com.opentransport.rdfrouteplanner.containers;

/**
 *
 * @author Nicola De Clercq
 */
public class Endpoints {
    
    private String departureStationId;
    private String arrivalStationId;

    public Endpoints(String departureStationId, String arrivalStationId) {
        this.departureStationId = departureStationId;
        this.arrivalStationId = arrivalStationId;
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
    
}