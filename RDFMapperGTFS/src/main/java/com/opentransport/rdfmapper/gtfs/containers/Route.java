package com.opentransport.rdfmapper.gtfs.containers;

/**
 *
 * @author Nicola De Clercq
 */
public class Route {
    
    private String name;

    public Route(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
    
}