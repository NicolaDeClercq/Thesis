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
public class BasicCSA1 {
    
    private HashMap<String,Station> stations;
    private List<Connection> connections;
    private List<SPARQLDAO> daos;
    
    public BasicCSA1() {
        stations = new HashMap<>();
        connections = new ArrayList<>();
        daos = new ArrayList<>();
        
        //DAO NMBS
        daos.add(new SPARQLDAO("http://localhost:8890/sparql","http://www.opentransport.com/NMBS","NMBS"));
        
        for (int i = 0; i < daos.size(); i++) {
            loadStationData(daos.get(i));
        }
    }
    
    public void calculateRoute(String departureStation, String arrivalStation, Calendar departureTime) {
        connections.clear();
        
        Calendar till = (Calendar) departureTime.clone();
        till.add(Calendar.HOUR_OF_DAY,6);
        loadConnectionData(daos.get(0),departureTime,till,0,0);
        
        //Sorteren niet meer nodig, wordt nu gedaan in de sparql query
//        long start1 = System.currentTimeMillis();
//        Collections.sort(connections,new Comparator<Connection>() {
//            public int compare(Connection o1, Connection o2) {
//                Calendar c1 = (Calendar) o1.getScheduledDepartureTime().clone();
//                Calendar c2 = (Calendar) o2.getScheduledDepartureTime().clone();
//                c1.add(Calendar.MINUTE,o1.getDepartureDelay());
//                c2.add(Calendar.MINUTE,o2.getDepartureDelay());
//                return c1.compareTo(c2);
//            }
//        });
//        long end1 = System.currentTimeMillis();
//        System.out.println("SORT CONNECTIONS (" + (end1 - start1) + " ms)");
        
        long start2 = System.currentTimeMillis();
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
        long end2 = System.currentTimeMillis();
        System.out.println("GET STATIONID FROM INPUT NAMES (" + (end2 - start2) + " ms)");
        
        long start3 = System.currentTimeMillis();
        stations.get(departureStationId).setEarliestArrivalTime(departureTime);
        for (int i = 0; i < connections.size(); i++) {
            Connection connection = connections.get(i);
            String connDepId = connection.getDepartureStationId();
            String connArrId = connection.getArrivalStationId();
            Station connDepStation = stations.get(connDepId);
            Station connArrStation = stations.get(connArrId);
            Calendar connDepTime = connection.getActualDepartureTime();
            Calendar connArrTime = connection.getActualArrivalTime();
            if (!connDepStation.getEarliestArrivalTime().after(connDepTime)
                    // Niet meer nodig, wordt behandeld in sparql query
                    // && !connDepTime.before(departureTime)
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
        long end3 = System.currentTimeMillis();
        System.out.println("PLAN ROUTE (" + (end3 - start3) + " ms)");
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