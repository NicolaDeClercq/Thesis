package com.opentransport.rdfmapper.gtfs;

import com.opentransport.rdfmapper.gtfs.containers.MapperInput;
import com.opentransport.rdfmapper.gtfs.containers.Route;
import com.opentransport.rdfmapper.gtfs.containers.Stop;
import com.opentransport.rdfmapper.gtfs.containers.StopTime;
import com.opentransport.rdfmapper.gtfs.containers.Trip;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nicola De Clercq
 */
public class Fetcher {
    
    private String agency;
    private final String csvRegex = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";
    private Map<String,Route> routes;
    private Map<String,Stop> stops;
    private Set<String> services;
    private Map<String,Trip> trips;
    
    public Fetcher(String agency) {
        this.agency = agency;
        this.routes = new HashMap<>();
        this.stops = new HashMap<>();
        this.services = new HashSet<>();
        this.trips = new HashMap<>();
        
        fetchRoutes();
        fetchStops();
    }
    
    public MapperInput fetch(String date) {
        services.clear();
        trips.clear();
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Calendar c = Calendar.getInstance();
        try {
            c.setTime(sdf.parse(date));
        } catch (ParseException ex) {
            Logger.getLogger(Fetcher.class.getName()).log(Level.SEVERE,null,ex);
        }
        System.out.println(c.getTime());
        
        fetchServices(date);
        fetchTrips();
        fetchStopTimes(c);
        
        return new MapperInput(new ArrayList(stops.values()),new ArrayList(trips.values()),agency);
    }
    
    private void fetchRoutes() {
        long start = System.currentTimeMillis();
        BufferedReader br;
        String line;
        try {
            br = new BufferedReader(new FileReader(agency + File.separator + "routes.txt"));
            int route_id = -1;
            int route_short_name = -1;
            int route_long_name = -1;
            int route_type = -1;
            int localized_route_type = -1;
            String[] header = br.readLine().split(csvRegex);
            for (int i = 0; i < header.length; i++) {
                String h = header[i].replaceAll("^\"|\"$","");
                if (h.equals("route_id")) {
                    route_id = i;
                }
                else if (h.equals("route_short_name")) {
                    route_short_name = i;
                }
                else if (h.equals("route_long_name")) {
                    route_long_name = i;
                }
                else if (h.equals("route_type")) {
                    route_type = i;
                }
                else if (h.equals("localized_route_type")) {
                    localized_route_type = i;
                }
            }
            while ((line = br.readLine()) != null) {
                String[] data = line.split(csvRegex);
                //Belbus routes worden niet opgenomen omdat er geen stoptijden voor bestaan
                if (!(localized_route_type != -1 && localized_route_type < data.length && data[localized_route_type].equals("Belbus"))) {
                    String name = "";
                    String type = data[route_type].replaceAll("^\"|\"$","");
                    if (type.equals("0")) {
                        name += "Tram ";
                    }
                    else if (type.equals("1")) {
                        name += "Metro ";
                    }
                    else if (type.equals("3")) {
                        name += "Bus ";
                    }
                    String shortName = data[route_short_name].replaceAll("^\"|\"$","");
                    String longName = data[route_long_name].replaceAll("^\"|\"$","");
                    if (longName.contains(shortName)) {
                        name += longName;
                    }
                    else {
                        name += shortName + " " + longName;
                    }
                    routes.put(data[route_id].replaceAll("^\"|\"$",""),new Route(name));
                }
            }
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(Fetcher.class.getName()).log(Level.SEVERE,null,ex);
        }
        long end = System.currentTimeMillis();
        System.out.println("FETCHED " + routes.size() + " ROUTES (" + (end - start) + " ms)");
    }
    
    private void fetchStops() {
        long start = System.currentTimeMillis();
        BufferedReader br;
        String line;
        try {
            br = new BufferedReader(new FileReader(agency + File.separator + "stops.txt"));
            int stop_id = -1;
            int stop_code = -1;
            int stop_name = -1;
            int stop_lat = -1;
            int stop_lon = -1;
            String[] header = br.readLine().split(csvRegex);
            for (int i = 0; i < header.length; i++) {
                String h = header[i].replaceAll("^\"|\"$","");
                if (h.equals("stop_id")) {
                    stop_id = i;
                }
                else if (h.equals("stop_code")) {
                    stop_code = i;
                }
                else if (h.equals("stop_name")) {
                    stop_name = i;
                }
                else if (h.equals("stop_lat")) {
                    stop_lat = i;
                }
                else if (h.equals("stop_lon")) {
                    stop_lon = i;
                }
            }
            while ((line = br.readLine()) != null) {
                String[] data = line.split(csvRegex);
                String name = data[stop_name].replaceAll("^\"|\"$","") + " [" + agency + "]";
                String id = data[stop_id].replaceAll("^\"|\"$","").replaceAll("DELIJN\\|","").replaceAll(":","").replaceAll("TEC","");
                if (stop_code != -1 && !data[stop_code].replaceAll("^\"|\"$","").isEmpty()) {
                    id = data[stop_code].replaceAll("^\"|\"$","");
                }
                stops.put(data[stop_id].replaceAll("^\"|\"$",""),new Stop(id,name,data[stop_lat].replaceAll("^\"|\"$",""),data[stop_lon].replaceAll("^\"|\"$","")));
            }
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(Fetcher.class.getName()).log(Level.SEVERE,null,ex);
        }
        long end = System.currentTimeMillis();
        System.out.println("FETCHED " + stops.size() + " STOPS (" + (end - start) + " ms)");
    }
    
    private void fetchServices(String serviceDate) {
        long start = System.currentTimeMillis();
        BufferedReader br;
        String line;
        try {
            br = new BufferedReader(new FileReader(agency + File.separator + "calendar_dates.txt"));
            int service_id = -1;
            int date = -1;
            String[] header = br.readLine().split(csvRegex);
            for (int i = 0; i < header.length; i++) {
                String h = header[i].replaceAll("^\"|\"$","");
                if (h.equals("service_id")) {
                    service_id = i;
                }
                else if (h.equals("date")) {
                    date = i;
                }
            }
            while ((line = br.readLine()) != null) {
                String[] data = line.split(csvRegex);
                if (data[date].replaceAll("^\"|\"$","").equals(serviceDate)) {
                    services.add(data[service_id].replaceAll("^\"|\"$",""));
                }
            }
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(Fetcher.class.getName()).log(Level.SEVERE,null,ex);
        }
        long end = System.currentTimeMillis();
        System.out.println("FETCHED " + services.size() + " SERVICES (" + (end - start) + " ms)");
    }
    
    private void fetchTrips() {
        long start = System.currentTimeMillis();
        BufferedReader br;
        String line;
        try {
            br = new BufferedReader(new FileReader(agency + File.separator + "trips.txt"));
            int route_id = -1;
            int service_id = -1;
            int trip_id = -1;
            int trip_headsign = -1;
            String[] header = br.readLine().split(csvRegex);
            for (int i = 0; i < header.length; i++) {
                String h = header[i].replaceAll("^\"|\"$","");
                if (h.equals("route_id")) {
                    route_id = i;
                }
                else if (h.equals("service_id")) {
                    service_id = i;
                }
                else if (h.equals("trip_id")) {
                    trip_id = i;
                }
                else if (h.equals("trip_headsign")) {
                    trip_headsign = i;
                }
            }
            while ((line = br.readLine()) != null) {
                String[] data = line.split(csvRegex);
                String route = data[route_id].replaceAll("^\"|\"$","");
                if (services.contains(data[service_id].replaceAll("^\"|\"$","")) && routes.containsKey(route)) {
                    String headsign = data[trip_headsign].replaceAll("^\"|\"$","") + " [" + agency + "]";
                    trips.put(data[trip_id].replaceAll("^\"|\"$",""),new Trip(headsign,routes.get(route)));
                }
            }
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(Fetcher.class.getName()).log(Level.SEVERE,null,ex);
        }
        long end = System.currentTimeMillis();
        System.out.println("FETCHED " + trips.size() + " TRIPS (" + (end - start) + " ms)");
    }
    
    private void fetchStopTimes(Calendar c) {
        long start = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        BufferedReader br;
        String line;
        int stopTimes = 0;
        try {
            br = new BufferedReader(new FileReader(agency + File.separator + "stop_times.txt"));
            int trip_id = -1;
            int stop_id = -1;
            int arrival_time = -1;
            int departure_time = -1;
            int stop_sequence = -1;
            String[] header = br.readLine().split(csvRegex);
            for (int i = 0; i < header.length; i++) {
                String h = header[i].replaceAll("^\"|\"$","");
                if (h.equals("trip_id")) {
                    trip_id = i;
                }
                else if (h.equals("stop_id")) {
                    stop_id = i;
                }
                else if (h.equals("arrival_time")) {
                    arrival_time = i;
                }
                else if (h.equals("departure_time")) {
                    departure_time = i;
                }
                else if (h.equals("stop_sequence")) {
                    stop_sequence = i;
                }
            }
            int lines = 0;
            while ((line = br.readLine()) != null) {
                if (lines % 100000 == 0) {
                    System.out.println("ALREADY READ " + lines + " STOPTIME LINES");
                }
                lines++;
                String[] data = line.split(csvRegex);
                String trip = data[trip_id].replaceAll("^\"|\"$","");
                if (trips.containsKey(trip)) {
                    stopTimes++;
                    String[] arr = data[arrival_time].replaceAll("^\"|\"$","").split(":");
                    String[] dep = data[departure_time].replaceAll("^\"|\"$","").split(":");
                    Calendar arrTime = (Calendar) c.clone();
                    Calendar depTime = (Calendar) c.clone();
                    arrTime.set(Calendar.HOUR_OF_DAY,Integer.parseInt(arr[0]));
                    arrTime.set(Calendar.MINUTE,Integer.parseInt(arr[1]));
                    arrTime.set(Calendar.SECOND,Integer.parseInt(arr[2]));
                    depTime.set(Calendar.HOUR_OF_DAY,Integer.parseInt(dep[0]));
                    depTime.set(Calendar.MINUTE,Integer.parseInt(dep[1]));
                    depTime.set(Calendar.SECOND,Integer.parseInt(dep[2]));
                    StopTime st = new StopTime(stops.get(data[stop_id].replaceAll("^\"|\"$","")),
                            sdf.format(arrTime.getTime()),sdf.format(depTime.getTime()));
                    trips.get(trip).getStops().put(Integer.parseInt(data[stop_sequence].replaceAll("^\"|\"$","")),st);
                }
            }
            br.close();
            System.out.println("READ " + lines + " STOPTIME LINES");
        } catch (IOException ex) {
            Logger.getLogger(Fetcher.class.getName()).log(Level.SEVERE,null,ex);
        }
        long end = System.currentTimeMillis();
        System.out.println("FETCHED " + stopTimes + " STOPTIMES (" + (end - start) + " ms)");
    }
}