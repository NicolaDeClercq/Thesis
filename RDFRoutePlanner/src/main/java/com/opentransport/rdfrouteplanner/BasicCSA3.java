package com.opentransport.rdfrouteplanner;

import com.opentransport.rdfrouteplanner.containers.Connection;
import com.opentransport.rdfrouteplanner.containers.Footpath;
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
public class BasicCSA3 {
    
    private HashMap<String,Station> stations;
    private List<Connection> connections;
    private HashMap<String,List<Footpath>> footpaths;
    private List<SPARQLDAO> daos;
    
    public BasicCSA3() {
        stations = new HashMap<>();
        connections = new ArrayList<>();
        footpaths = new HashMap<>();
        daos = new ArrayList<>();
        
        //DAO NMBS
        daos.add(new SPARQLDAO("http://localhost:8890/sparql","http://www.opentransport.com/NMBS","NMBS"));
        
        for (int i = 0; i < daos.size(); i++) {
            loadStationData(daos.get(i));
        }
        
        //2 KM radius
        calculateFootpaths(2000);
    }
    
    public void calculateRoute(String departureStation, String arrivalStation, Calendar departureTime, int minimumChangeTime) {
        connections.clear();
        
        Calendar till = (Calendar) departureTime.clone();
        till.add(Calendar.HOUR_OF_DAY,6);
        loadConnectionData(daos.get(0),departureTime,till,0,0);
        
        long start1 = System.currentTimeMillis();
        String departureStationId = "";
        String arrivalStationId = "";
        int depDist = Integer.MAX_VALUE;
        int arrDist = Integer.MAX_VALUE;
        for (Station station : stations.values()) {
            String name = station.getName().toLowerCase();
            int depD = getLevenshteinDistance(departureStation.toLowerCase(),name);
            int arrD = getLevenshteinDistance(arrivalStation.toLowerCase(),name);
            if (depD < depDist) {
                depDist = depD;
                departureStationId = station.getId();
            }
            if (arrD < arrDist) {
                arrDist = arrD;
                arrivalStationId = station.getId();
            }
//            if (name.contains(departureStation.toLowerCase())) {
//                departureStationId = station.getId();
//            }
//            if (name.contains(arrivalStation.toLowerCase())) {
//                arrivalStationId = station.getId();
//            }
        }
        long end1 = System.currentTimeMillis();
        System.out.println("GET STATIONID FROM INPUT NAMES (" + (end1 - start1) + " ms)");
        
        long start2 = System.currentTimeMillis();
        Calendar dt = (Calendar) departureTime.clone();
        dt.add(Calendar.MINUTE,-minimumChangeTime);
        stations.get(departureStationId).setEarliestArrivalTime(dt);
        List<Footpath> foots1 = footpaths.get(departureStationId);
        for (int i = 0; i < foots1.size(); i++) {
            Footpath f = foots1.get(i);
            Connection connection = new Connection(f,(Calendar) departureTime.clone());
            stations.get(f.getArrivalStationId()).setEarliestArrivalTime(connection.getActualArrivalTime());
            stations.get(f.getArrivalStationId()).setPrevious(connection);
        }
        for (int i = 0; i < connections.size(); i++) {
            Connection connection = connections.get(i);
            String connDepId = connection.getDepartureStationId();
            String connArrId = connection.getArrivalStationId();
            Station connDepStation = stations.get(connDepId);
            Station connArrStation = stations.get(connArrId);
            Calendar connDepTime = connection.getActualDepartureTime();
            Calendar connArrTime = connection.getActualArrivalTime();
            if (stations.get(arrivalStationId).getEarliestArrivalTime().before(connDepTime)) {
                System.out.println("STOPPED AT CONNECTION " + i);
                break;
            }
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
                    List<Footpath> foots2 = footpaths.get(connArrId);
                    for (int j = 0; j < foots2.size(); j++) {
                        Footpath f = foots2.get(j);
                        Calendar dtf = (Calendar) connArrTime.clone();
                        dtf.add(Calendar.MINUTE,minimumChangeTime);
                        Calendar atf = (Calendar) dtf.clone();
                        atf.add(Calendar.MINUTE,f.getDuration());
                        String fArrivalStationId = f.getArrivalStationId();
                        if (atf.before(stations.get(fArrivalStationId).getEarliestArrivalTime())) {
                            Connection fConnection = new Connection(f,dtf);
                            stations.get(fArrivalStationId).setEarliestArrivalTime(atf);
                            stations.get(fArrivalStationId).setPrevious(fConnection);
                        }
                    }
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
            if (connection.getRoute().equals("WALK")) {
                System.out.format("%-30s %-5s  %-4s  %-2s --> %-30s %-5s  %-4s  %-2s [%-1s %-1sm]\n",
                    dep.getName(),sdf.format(connection.getScheduledDepartureTime().getTime()),"","",
                    arr.getName(),sdf.format(connection.getScheduledArrivalTime().getTime()),"","",
                    "Wandel",connection.getDistance());
            }
            else {
                System.out.format("%-30s %-5s  +%-3s  %-2s --> %-30s %-5s  +%-3s  %-2s [%-1s --> %-1s]\n",
                    dep.getName(),sdf.format(connection.getScheduledDepartureTime().getTime()),
                    connection.getDepartureDelay(),connection.getDeparturePlatform(),arr.getName(),
                    sdf.format(connection.getScheduledArrivalTime().getTime()),connection.getArrivalDelay(),
                    connection.getArrivalPlatform(),connection.getRoute(),connection.getHeadsign());
            }
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
    
    private void calculateFootpaths(int radius) {
        long start = System.currentTimeMillis();
        footpaths.clear();
        int counter = 0;
        List<Station> s = new ArrayList(stations.values());
        for (int i = 0; i < s.size(); i++) {
            Station s1 = s.get(i);
            footpaths.put(s1.getId(),new ArrayList<Footpath>());
            for (int j = 0; j < s.size(); j++) {
                if (i != j) {
                    Station s2 = s.get(j);
                    int hd = getHaversineDistance(s1.getLatitude(),s1.getLongitude(),s2.getLatitude(),s2.getLongitude());
                    // estimate real distance --> increase by 25%
                    int d = (int) Math.round(hd * 1.25);
                    if (d <= radius) {
                        int dur = (int) Math.round((d / 5000.0) * 60);
                        Footpath f = new Footpath(s1.getId(),s2.getId(),d,dur);
                        footpaths.get(s1.getId()).add(f);
                        counter++;
//                        System.out.println(s1.getName() + " --> " + s2.getName() + " (" + d + "m, " + dur + " min)");
                    }
                }
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("FOUND " + counter + " FOOTPATHS (" + (end - start) + " ms)");
    }
    
    private int getHaversineDistance(double latitudeA, double longitudeA, double latitudeB, double longitudeB) {
        //Average radious of the earth in meters
        final int R = 6371000;
        
        double latDistance = toRadians(latitudeB - latitudeA);
        double lonDistance = toRadians(longitudeB - longitudeA);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                   Math.cos(toRadians(latitudeA)) * Math.cos(toRadians(latitudeB)) *
                   Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = R * c;
         
        return (int) Math.round(distance);
    }
    
    private double toRadians(double value) {
        return value * Math.PI / 180;
    }
    
    private int getLevenshteinDistance(String s0, String s1) {
	int len0 = s0.length()+1;
	int len1 = s1.length()+1;
 
	// the array of distances
	int[] cost = new int[len0];
	int[] newcost = new int[len0];
 
	// initial cost of skipping prefix in String s0
	for(int i=0;i<len0;i++) cost[i]=i;
 
	// dynamicaly computing the array of distances
 
	// transformation cost for each letter in s1
	for(int j=1;j<len1;j++) {
 
		// initial cost of skipping prefix in String s1
		newcost[0]=j-1;
 
		// transformation cost for each letter in s0
		for(int i=1;i<len0;i++) {
 
			// matching current letters in both strings
			int match = (s0.charAt(i-1)==s1.charAt(j-1))?0:1;
 
			// computing cost for each transformation
			int cost_replace = cost[i-1]+match;
			int cost_insert  = cost[i]+1;
			int cost_delete  = newcost[i-1]+1;
 
			// keep minimum cost
			newcost[i] = Math.min(Math.min(cost_insert, cost_delete),cost_replace );
		}
 
		// swap cost/newcost arrays
		int[] swap=cost; cost=newcost; newcost=swap;
	}
 
	// the distance is the cost for transforming all letters in both strings
	return cost[len0-1];
    }
    
}