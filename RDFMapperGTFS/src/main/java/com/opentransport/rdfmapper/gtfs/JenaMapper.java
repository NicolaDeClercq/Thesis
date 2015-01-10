package com.opentransport.rdfmapper.gtfs;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.opentransport.rdfmapper.gtfs.containers.MapperInput;
import com.opentransport.rdfmapper.gtfs.containers.Stop;
import com.opentransport.rdfmapper.gtfs.containers.StopTime;
import com.opentransport.rdfmapper.gtfs.containers.Trip;
import java.io.FileWriter;
import java.io.IOException;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nicola De Clercq
 */
public class JenaMapper {
    
    public static void map(MapperInput mi, Calendar startTime, int hours) {
        Map<String,String> stations = new HashMap<>();
        
        Calendar endTime = (Calendar) startTime.clone();
        endTime.add(Calendar.HOUR_OF_DAY,hours);
        
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(JenaMapper.class.getName()).log(Level.SEVERE,null,ex);
        }
        
        List<Stop> stops = mi.getStops();
        List<Trip> trips = mi.getTrips();
        String agency = mi.getAgency().replaceAll(" ","");
        
        Model model = ModelFactory.createDefaultModel();
        String rplod = "http://semweb.mmlab.be/ns/rplod/";
        model.setNsPrefix("rplod",rplod);
        String transit = "http://vocab.org/transit/terms/";
        model.setNsPrefix("transit",transit);
        String geo = "http://www.w3.org/2003/01/geo/wgs84_pos#";
        model.setNsPrefix("geo",geo);
        String rdfs = "http://www.w3.org/2000/01/rdf-schema#";
        model.setNsPrefix("rdfs",rdfs);
        String iRail = "http://irail.be/";
        String stationsURI = iRail + "stations/" + agency + "/";
        
        Property label = model.createProperty(rdfs + "label");
        Property longitude = model.createProperty(geo + "long");
        Property latitude = model.createProperty(geo + "lat");
        Property departureStop = model.createProperty(rplod + "stop");
        Property scheduledDepartureTime = model.createProperty(rplod + "scheduledDepartureTime");
        Property delay = model.createProperty(rplod + "delay");
        Property actualDepartureTime = model.createProperty(rplod + "actualDepartureTime");
        Property headsign = model.createProperty(transit + "headsign");
        Property routeLabel = model.createProperty(rplod + "routeLabel");
        Property platform = model.createProperty(rplod + "platform");
        Property nextStopScheduledArrivalTime = model.createProperty(rplod + "nextStopScheduledArrivalTime");
        Property nextStopDelay = model.createProperty(rplod + "nextStopDelay");
        Property nextStopActualArrivalTime = model.createProperty(rplod + "nextStopActualArrivalTime");
        Property nextStopPlatform = model.createProperty(rplod + "nextStopPlatform");
        Property nextStop = model.createProperty(rplod + "nextStop");
        
        SimpleDateFormat isoSDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        SimpleDateFormat uriSDF = new SimpleDateFormat("yyyyMMddHHmm");
        
        long start1 = System.currentTimeMillis();
        
        for (int i = 0; i < stops.size(); i++) {
            Stop stop = stops.get(i);
            String stopId = stop.getId();
            String stationId = stationsURI + stopId;
            Resource station = model.createResource(stationId);
            station.addProperty(label,stop.getName());
            station.addProperty(longitude,stop.getLongitude());
            station.addProperty(latitude,stop.getLatitude());
            stations.put(stopId,stationId);
        }
            
        for (int i = 0; i < trips.size(); i++) {
            System.out.println("MAPPING TRIP " + i);
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
                String stationId = stations.get(stopId);
                String nextStationId = stations.get(nextStopId);
                String departureTime = current.getDepartureTime();
                String arrivalTime = next.getArrivalTime();
                
                String uriDepT = "";
                Calendar depT = Calendar.getInstance();
                try {
                    Date dep = isoSDF.parse(departureTime);
                    uriDepT = uriSDF.format(dep);
                    depT.setTime(dep);
                } catch (ParseException ex) {
                    Logger.getLogger(JenaMapper.class.getName()).log(Level.SEVERE,null,ex);
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
                    Resource stationDeparture = model.createResource(stationId + "/departures/" + uriDepT + hashedString);
                    Resource station = model.createResource(stationId);
                    stationDeparture.addProperty(departureStop,station);
                    stationDeparture.addProperty(scheduledDepartureTime,departureTime);
                    stationDeparture.addProperty(delay,"");
                    stationDeparture.addProperty(actualDepartureTime,departureTime);
                    stationDeparture.addProperty(headsign,dest);
                    stationDeparture.addProperty(routeLabel,route);
                    stationDeparture.addProperty(platform,"");
                    stationDeparture.addProperty(nextStopScheduledArrivalTime,arrivalTime);
                    stationDeparture.addProperty(nextStopDelay,"");
                    stationDeparture.addProperty(nextStopActualArrivalTime,arrivalTime);
                    stationDeparture.addProperty(nextStopPlatform,"");
                    Resource nextStopURI = model.createResource(nextStationId);
                    stationDeparture.addProperty(nextStop,nextStopURI);
                }
            }
        }
        
        long end1 = System.currentTimeMillis();
        System.out.println("RDF MAPPING (" + (end1 - start1) + " ms)");
        long start2 = System.currentTimeMillis();
        
        try {
            FileWriter fw = new FileWriter(agency + ".ttl");
            model.write(fw,"TTL");
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(JenaMapper.class.getName()).log(Level.SEVERE,null,ex);
        }
        
        long end2 = System.currentTimeMillis();
        System.out.println("WRITING TO FILE (" + (end2 - start2) + " ms)");
        
    }
    
}