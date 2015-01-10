package com.opentransport.rdfmapper.gtfs;

import com.opentransport.rdfmapper.gtfs.containers.Departure;
import com.opentransport.rdfmapper.gtfs.containers.MapperInput;
import com.opentransport.rdfmapper.gtfs.containers.Station;
import com.opentransport.rdfmapper.gtfs.containers.Stop;
import com.opentransport.rdfmapper.gtfs.containers.StopTime;
import com.opentransport.rdfmapper.gtfs.containers.Trip;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nicola De Clercq
 */
public class SortedMapper {
    
    private final List<Station> stations;
    private final List<Departure> departures;
    private List<byte[]> turtleStations;
    private List<byte[]> turtleDepartures;
    private List<byte[]> linkedcsvStations;
    private List<byte[]> linkedcsvDepartures;
    private final long lastModified;
    
    public SortedMapper(MapperInput mi, Calendar startTime, int hours) {
        stations = new ArrayList<>();
        departures = new ArrayList<>();
        map(mi,startTime,hours);
        
        mapTurtle();
        mapLinkedcsv();
        
        stations.clear();
        departures.clear();
        
        lastModified = System.currentTimeMillis();
    }
    
    private void map(MapperInput mi, Calendar startTime, int hours) {
        SortedMap<Calendar,List<Departure>> departuresMap = new TreeMap<>();
        
        Map<String,String> stationsMap = new HashMap<>();
        
        Calendar endTime = (Calendar) startTime.clone();
        endTime.add(Calendar.HOUR_OF_DAY,hours);
        
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(SortedMapper.class.getName()).log(Level.SEVERE,null,ex);
        }
        
        List<Stop> stops = mi.getStops();
        List<Trip> trips = mi.getTrips();
        String agency = mi.getAgency().replaceAll(" ","");
        String stationsURI = "http://irail.be/stations/" + agency + "/";
        
        SimpleDateFormat isoSDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        SimpleDateFormat uriSDF = new SimpleDateFormat("yyyyMMddHHmm");
        
        long start = System.currentTimeMillis();
        long sortingTime = 0;
        
        for (int i = 0; i < stops.size(); i++) {
            Stop stop = stops.get(i);
            String stopId = stop.getId();
            String stationId = stationsURI + stopId;
            stations.add(new Station(stationId,stop.getName(),stop.getLatitude(),
                    stop.getLongitude()));
            stationsMap.put(stopId,stationId);
        }
            
        for (int i = 0; i < trips.size(); i++) {
//            System.out.println("MAPPING TRIP " + i);
            Trip trip = trips.get(i);
            String route = trip.getRoute().getName();
            String dest = trip.getHeadsign();
            List<StopTime> stopTimes = new ArrayList(trip.getStops().values());
            
            for (int j = 0; j < stopTimes.size() - 1; j++) {
                StopTime current = stopTimes.get(j);
                StopTime next = stopTimes.get(j + 1);
                String stopId = current.getStop().getId();
                String nextStopId = next.getStop().getId();
                // De Lijn geeft soms 2 keer dezelfde halte na elkaar
                while (stopId.equals(nextStopId) && (j + 2 < stopTimes.size())) {
                    j++;
                    next = stopTimes.get(j + 1);
                    nextStopId = next.getStop().getId();
                }
                String stationId = stationsMap.get(stopId);
                String nextStationId = stationsMap.get(nextStopId);
                String departureTime = current.getDepartureTime();
                String arrivalTime = next.getArrivalTime();
                
                String uriDepT = "";
                Calendar depT = Calendar.getInstance();
                try {
                    Date dep = isoSDF.parse(departureTime);
                    uriDepT = uriSDF.format(dep);
                    depT.setTime(dep);
                } catch (ParseException ex) {
                    Logger.getLogger(SortedMapper.class.getName()).log(Level.SEVERE,null,ex);
                }
                
                if (!depT.before(startTime) && !depT.after(endTime)) {
                    String hashString = route + dest;
                    md.reset();
                    md.update(hashString.getBytes());
                    byte[] digest = md.digest();
                    String hashedString = "";
                    for (int k = 0; k < digest.length; k++) {
                        hashedString += Integer.toString((digest[k] & 0xff) + 0x100,16).substring(1);
                    }
                    String departureId = stationId + "/departures/" + uriDepT + hashedString;
                    long sort = System.currentTimeMillis();
                    if (!departuresMap.containsKey(depT)) {
                        departuresMap.put(depT,new ArrayList<Departure>());
                    }
                    departuresMap.get(depT).add(new Departure(departureId,stationId,
                            departureTime,"",departureTime,"",nextStationId,
                            arrivalTime,"",arrivalTime,"",route,dest));
                    sortingTime += System.currentTimeMillis() - sort;
                }
            }
        }
        
        for (List<Departure> l : departuresMap.values()) {
            departures.addAll(l);
        }
        
        long end = System.currentTimeMillis();
        System.out.println(Calendar.getInstance().getTime() + ": INTERNAL MAPPING (" + (end - start) + " ms)");
        System.out.println("NUMBER OF DEPARTURES: " + departures.size());
        System.out.println("NUMBER OF DEPARTURE TRIPLES: " + departures.size()*12);
        System.out.println("SORTING TIME: " + sortingTime + " ms");
    }
    
    private void mapTurtle() {
        long start = System.currentTimeMillis();
        
        turtleStations = new ArrayList<>();
        StringBuilder stationsBuilder = new StringBuilder();
        stationsBuilder.append("@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n"
                + "@prefix geo:   <http://www.w3.org/2003/01/geo/wgs84_pos#> .");
        for (int i = 0; i < stations.size(); i++) {
            Station station = stations.get(i);
            stationsBuilder.append("\n\n<").append(station.getId())
                    .append(">\n" + "        rdfs:label   \"").append(station.getName())
                    .append("\" ;\n" + "        geo:lat      \"")
                    .append(station.getLatitude())
                    .append("\" ;\n" + "        geo:long     \"")
                    .append(station.getLongitude()).append("\" .");
            turtleStations.add(stationsBuilder.toString().getBytes(StandardCharsets.UTF_8));
            stationsBuilder.setLength(0);
        }
        
        turtleDepartures = new ArrayList<>();
        StringBuilder departuresBuilder = new StringBuilder();
        departuresBuilder.append("@prefix rplod: <http://semweb.mmlab.be/ns/rplod/> .\n"
                + "@prefix transit: <http://vocab.org/transit/terms/> .");
        for (int i = 0; i < departures.size(); i++) {
            Departure departure = departures.get(i);
            departuresBuilder.append("\n\n<").append(departure.getDepartureId())
                    .append(">\n" + "        rplod:stop                           <")
                    .append(departure.getDepartureStationId())
                    .append("> ;\n" + "        rplod:scheduledDepartureTime         \"")
                    .append(departure.getScheduledDepartureTime())
                    .append("\" ;\n" + "        rplod:delay                          \"")
                    .append(departure.getDepartureDelay())
                    .append("\" ;\n" + "        rplod:actualDepartureTime            \"")
                    .append(departure.getActualDepartureTime())
                    .append("\" ;\n" + "        rplod:platform                       \"")
                    .append(departure.getDeparturePlatform())
                    .append("\" ;\n" + "        rplod:nextStop                       <")
                    .append(departure.getArrivalStationId())
                    .append("> ;\n" + "        rplod:nextStopScheduledArrivalTime   \"")
                    .append(departure.getScheduledArrivalTime())
                    .append("\" ;\n" + "        rplod:nextStopDelay                  \"")
                    .append(departure.getArrivalDelay())
                    .append("\" ;\n" + "        rplod:nextStopActualArrivalTime      \"")
                    .append(departure.getActualArrivalTime())
                    .append("\" ;\n" + "        rplod:nextStopPlatform               \"")
                    .append(departure.getArrivalPlatform())
                    .append("\" ;\n" + "        rplod:routeLabel                     \"")
                    .append(departure.getRoute())
                    .append("\" ;\n" + "        transit:headsign                     \"")
                    .append(departure.getHeadsign()).append("\" .");
            turtleDepartures.add(departuresBuilder.toString().getBytes(StandardCharsets.UTF_8));
            departuresBuilder.setLength(0);
        }
        
        long end = System.currentTimeMillis();
        System.out.println(Calendar.getInstance().getTime() + ": TURTLE MAPPING (" + (end - start) + " ms)");
    }
    
    private void mapLinkedcsv() {
        long start = System.currentTimeMillis();
        
        linkedcsvStations = new ArrayList<>();
        StringBuilder stationsBuilder = new StringBuilder();
        stationsBuilder.append("\"$id\",\"http://www.w3.org/2000/01/rdf-schema#label\","
                + "\"http://www.w3.org/2003/01/geo/wgs84_pos#lat\","
                + "\"http://www.w3.org/2003/01/geo/wgs84_pos#long\"");
        for (int i = 0; i < stations.size(); i++) {
            Station station = stations.get(i);
            stationsBuilder.append("\n\"").append(station.getId())
                    .append("\",\"").append(station.getName()).append("\",\"")
                    .append(station.getLatitude()).append("\",\"")
                    .append(station.getLongitude()).append("\"");
            linkedcsvStations.add(stationsBuilder.toString().getBytes(StandardCharsets.UTF_8));
            stationsBuilder.setLength(0);
        }
        
        linkedcsvDepartures = new ArrayList<>();
        StringBuilder departuresBuilder = new StringBuilder();
        departuresBuilder.append("\"$id\",\"http://semweb.mmlab.be/ns/rplod/stop\","
                + "\"http://semweb.mmlab.be/ns/rplod/scheduledDepartureTime\","
                + "\"http://semweb.mmlab.be/ns/rplod/delay\","
                + "\"http://semweb.mmlab.be/ns/rplod/actualDepartureTime\","
                + "\"http://semweb.mmlab.be/ns/rplod/platform\","
                + "\"http://semweb.mmlab.be/ns/rplod/nextStop\","
                + "\"http://semweb.mmlab.be/ns/rplod/nextStopScheduledArrivalTime\","
                + "\"http://semweb.mmlab.be/ns/rplod/nextStopDelay\","
                + "\"http://semweb.mmlab.be/ns/rplod/nextStopActualArrivalTime\","
                + "\"http://semweb.mmlab.be/ns/rplod/nextStopPlatform\","
                + "\"http://semweb.mmlab.be/ns/rplod/routeLabel\","
                + "\"http://vocab.org/transit/terms/headsign\"");
        for (int i = 0; i < departures.size(); i++) {
            Departure departure = departures.get(i);
            departuresBuilder.append("\n\"").append(departure.getDepartureId())
                    .append("\",\"").append(departure.getDepartureStationId())
                    .append("\",\"").append(departure.getScheduledDepartureTime())
                    .append("\",\"").append(departure.getDepartureDelay())
                    .append("\",\"").append(departure.getActualDepartureTime())
                    .append("\",\"").append(departure.getDeparturePlatform())
                    .append("\",\"").append(departure.getArrivalStationId())
                    .append("\",\"").append(departure.getScheduledArrivalTime())
                    .append("\",\"").append(departure.getArrivalDelay())
                    .append("\",\"").append(departure.getActualArrivalTime())
                    .append("\",\"").append(departure.getArrivalPlatform())
                    .append("\",\"").append(departure.getRoute())
                    .append("\",\"").append(departure.getHeadsign()).append("\"");
            linkedcsvDepartures.add(departuresBuilder.toString().getBytes(StandardCharsets.UTF_8));
            departuresBuilder.setLength(0);
        }
        
        long end = System.currentTimeMillis();
        System.out.println(Calendar.getInstance().getTime() + ": LINKED CSV MAPPING (" + (end - start) + " ms)");
    }

    public List<byte[]> getTurtleStations() {
        return turtleStations;
    }

    public List<byte[]> getTurtleDepartures() {
        return turtleDepartures;
    }

    public List<byte[]> getLinkedcsvStations() {
        return linkedcsvStations;
    }

    public List<byte[]> getLinkedcsvDepartures() {
        return linkedcsvDepartures;
    }

    public long getLastModified() {
        return lastModified;
    }

}