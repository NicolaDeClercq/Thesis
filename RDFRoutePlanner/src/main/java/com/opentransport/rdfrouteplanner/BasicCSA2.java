package com.opentransport.rdfrouteplanner;

import com.opentransport.rdfrouteplanner.containers.Connection;
import com.opentransport.rdfrouteplanner.containers.Station;
import com.opentransport.rdfrouteplanner.dao.SPARQLDAO;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Nicola De Clercq
 */
public class BasicCSA2 {
    
    private HashMap<String,Station> stations;
    private List<Connection> connections;
    private List<SPARQLDAO> daos;
    
    public BasicCSA2() {
        stations = new HashMap<>();
        connections = new ArrayList<>();
        daos = new ArrayList<>();
        
        //DAO NMBS
        daos.add(new SPARQLDAO("http://localhost:8890/sparql","http://www.opentransport.com/NMBS","NMBS"));
        
        for (int i = 0; i < daos.size(); i++) {
            loadStationData(daos.get(i));
        }
    }
    
    public void calculateRoute(String departureStation, String arrivalStation, Calendar departureTime, int minimumChangeTime) {
        connections.clear();
        
        Calendar till = (Calendar) departureTime.clone();
        till.add(Calendar.HOUR_OF_DAY,6);
        loadConnectionData(daos.get(0),departureTime,till,0,0);
        
        long start1 = System.currentTimeMillis();
        String departureStationId = "";
        String arrivalStationId = "";
        for (Station station : stations.values()) {
            String name = station.getName().toLowerCase();
            if (name.contains(departureStation.toLowerCase())) {
                departureStationId = station.getId();
            }
            if (name.contains(arrivalStation.toLowerCase())) {
                arrivalStationId = station.getId();
            }
        }
        long end1 = System.currentTimeMillis();
        System.out.println("GET STATIONID FROM INPUT NAMES (" + (end1 - start1) + " ms)");
        
        long start2 = System.currentTimeMillis();
        Calendar dt = (Calendar) departureTime.clone();
        dt.add(Calendar.MINUTE,-minimumChangeTime);
        stations.get(departureStationId).setEarliestArrivalTime(dt);
        for (int i = 0; i < connections.size(); i++) {
            Connection connection = connections.get(i);
            String connDepId = connection.getDepartureStationId();
            String connArrId = connection.getArrivalStationId();
            Station connDepStation = stations.get(connDepId);
            Station connArrStation = stations.get(connArrId);
            Calendar connDepTime = connection.getActualDepartureTime();
            Calendar connArrTime = connection.getActualArrivalTime();
            Calendar eAT = (Calendar) connDepStation.getEarliestArrivalTime().clone();
            eAT.add(Calendar.MINUTE,minimumChangeTime);
            Connection previous = connDepStation.getPrevious();
            if ((!eAT.after(connDepTime)
                || ((previous != null) && !connDepStation.getEarliestArrivalTime().after(connDepTime)
                && previous.getRoute().equals(connection.getRoute())
                && previous.getHeadsign().equals(connection.getHeadsign())))
                && connArrTime.before(connArrStation.getEarliestArrivalTime())) {
                    stations.get(connArrId).setEarliestArrivalTime(connArrTime);
                    stations.get(connArrId).setPrevious(connection);
            }
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        List<Connection> reversePath = new ArrayList<>();
        Station s = stations.get(arrivalStationId);
        while (s.getPrevious() != null) {
            reversePath.add(s.getPrevious());
            s = stations.get(s.getPrevious().getDepartureStationId());
        }
        for (int i = reversePath.size() - 1; i >= 0; i--) {
            Connection connection = reversePath.get(i);
            Station dep = stations.get(connection.getDepartureStationId());
            Station arr = stations.get(connection.getArrivalStationId());
            System.out.format("%-30s %-5s  +%-3s  %-2s --> %-30s %-5s  +%-3s  %-2s [%-1s --> %-1s]\n",
                    dep.getName(),sdf.format(connection.getScheduledDepartureTime().getTime()),
                    connection.getDepartureDelay(),connection.getDeparturePlatform(),arr.getName(),
                    sdf.format(connection.getScheduledArrivalTime().getTime()),connection.getArrivalDelay(),
                    connection.getArrivalPlatform(),connection.getRoute(),connection.getHeadsign());
        }
        long end2 = System.currentTimeMillis();
        System.out.println("PLAN ROUTE (" + (end2 - start2) + " ms)");
    }
    
    private void loadStationData(SPARQLDAO dao) {
        long start = System.currentTimeMillis();
        dao.loadStationData(stations);
        long end = System.currentTimeMillis();
        System.out.println("READ " + stations.size() + " " + dao.getName() + " STATIONS (" + (end - start) + " ms)");
    }
    
    private void loadConnectionData(SPARQLDAO dao, Calendar from, Calendar to, int limit, int offset) {
        long start = System.currentTimeMillis();
        dao.loadConnectionData(connections,from,to,limit,offset);
        long end = System.currentTimeMillis();
        System.out.println("READ " + connections.size() + " " + dao.getName() + " CONNECTIONS (" + (end - start) + " ms)");
    }
    
}