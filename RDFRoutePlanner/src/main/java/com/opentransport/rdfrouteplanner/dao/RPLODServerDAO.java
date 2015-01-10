package com.opentransport.rdfrouteplanner.dao;

import com.opentransport.rdfrouteplanner.containers.Connection;
import com.opentransport.rdfrouteplanner.containers.Station;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 *
 * @author Nicola De Clercq
 */
public class RPLODServerDAO {
    
    private final String serverURL;
    private final String name;
    private final String turtle = "text/turtle";
    private final String csv = "text/csv";
    private final String contentType = csv;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    private final Pattern turtleRegex = Pattern.compile("[^\\s\"<>]+|\"[^\"]*\"|<[^<>]*>");
    private final String csvRegex = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";
    
    public RPLODServerDAO(String serverURL, String name) {
        this.serverURL = serverURL;
        this.name = name;
    }

    public void loadStationData(HashMap<String,Station> stations) {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet request = new HttpGet(serverURL + "/stations");
        request.addHeader("Accept",contentType);
        try {
            CloseableHttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent()));
                if (entity.getContentType().getValue().contains(turtle)) {
                    parseTurtleStations(br,stations);
                }
                else if (entity.getContentType().getValue().contains(csv)) {
                    parseLinkedcsvStations(br,stations);
                }
                br.close();
            }
            else {
                System.out.println("HTTP ERROR " + response.getStatusLine().getStatusCode());
            }
            response.close();
            client.close();
        } catch (IOException ex) {
            Logger.getLogger(RPLODServerDAO.class.getName()).log(Level.SEVERE,null,ex);
        }
    }
    
    public void loadConnectionData(List<Connection> connections, Calendar from, Calendar till) {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet request = new HttpGet(serverURL + "/departures");
        request.addHeader("Accept",contentType);
        try {
            CloseableHttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent()));
                if (entity.getContentType().getValue().contains(turtle)) {
                    parseTurtleDepartures(br,connections,from,till);
                }
                else if (entity.getContentType().getValue().contains(csv)) {
                    parseLinkedcsvDepartures(br,connections,from,till);
                }
                br.close();
            }
            else {
                System.out.println("HTTP ERROR " + response.getStatusLine().getStatusCode());
            }
            response.close();
            client.close();
        } catch (IOException ex) {
            Logger.getLogger(RPLODServerDAO.class.getName()).log(Level.SEVERE,null,ex);
        }
    }
    
    private void parseTurtleStations(BufferedReader br, HashMap<String,Station> stations) throws IOException {
        String line;
        String rdfs = "";
        String geo = "";
        while ((line = br.readLine()) != null) {
            Matcher matcher = turtleRegex.matcher(line);
            while (!matcher.find()) {
                matcher.reset(br.readLine());
            }
            String match = matcher.group().replaceAll("^\"|\"$|^<|>$","");
            if (match.equalsIgnoreCase("@prefix") || match.equalsIgnoreCase("prefix")) {
                matcher.find();
                String prefix = matcher.group().replaceAll("^\"|\"$|^<|>$","");
                matcher.find();
                String uri = matcher.group().replaceAll("^\"|\"$|^<|>$","");
                if (uri.equals("http://www.w3.org/2000/01/rdf-schema#")) {
                    rdfs = prefix;
                }
                else if (uri.equals("http://www.w3.org/2003/01/geo/wgs84_pos#")) {
                    geo = prefix;
                }
            }
            else if (match.startsWith("http://irail.be/stations/")) {
                String id = match;
                String stationName = "";
                double latitude = 0;
                double longitude = 0;
                int i = 0;
                while (i != 3) {
                    while (!matcher.find()) {
                        matcher.reset(br.readLine());
                    }
                    String pred = matcher.group().replaceAll("^\"|\"$|^<|>$","");
                    while (!matcher.find()) {
                        matcher.reset(br.readLine());
                    }
                    if (pred.equals(rdfs + "label")) {
                        stationName = matcher.group().replaceAll("^\"|\"$|^<|>$","");
                        i++;
                    }
                    else if (pred.equals(geo + "lat")) {
                        latitude = Double.parseDouble(matcher.group().replaceAll("^\"|\"$|^<|>$",""));
                        i++;
                    }
                    else if (pred.equals(geo + "long")) {
                        longitude = Double.parseDouble(matcher.group().replaceAll("^\"|\"$|^<|>$",""));
                        i++;
                    }
                    while (!matcher.find()) {
                        matcher.reset(br.readLine());
                    }
                }
                while (!matcher.group().equals(".")) {
                    while (!matcher.find()) {
                        matcher.reset(br.readLine());
                    }
                }
                Station station = new Station(id,stationName,latitude,longitude);
                stations.put(station.getId(),station);
            }
        }
    }
    
    private void parseTurtleDepartures(BufferedReader br, List<Connection> connections, Calendar from, Calendar till) throws IOException {
        String line;
        String rplod = "";
        String transit = "";
        while ((line = br.readLine()) != null) {
            Matcher matcher = turtleRegex.matcher(line);
            while (!matcher.find()) {
                matcher.reset(br.readLine());
            }
            String match = matcher.group().replaceAll("^\"|\"$|^<|>$","");
            if (match.equalsIgnoreCase("@prefix") || match.equalsIgnoreCase("prefix")) {
                matcher.find();
                String prefix = matcher.group().replaceAll("^\"|\"$|^<|>$","");
                matcher.find();
                String uri = matcher.group().replaceAll("^\"|\"$|^<|>$","");
                if (uri.equals("http://semweb.mmlab.be/ns/rplod/")) {
                    rplod = prefix;
                }
                else if (uri.equals("http://vocab.org/transit/terms/")) {
                    transit = prefix;
                }
            }
            else if (match.startsWith("http://irail.be/stations/") && match.contains("/departures/")) {
                String departureStationId = "";
                Calendar scheduledDepartureTime = Calendar.getInstance();
                int departureDelay = 0;
                Calendar actualDepartureTime = Calendar.getInstance();
                String departurePlatform = "";
                String arrivalStationId = "";
                Calendar scheduledArrivalTime = Calendar.getInstance();
                int arrivalDelay = 0;
                Calendar actualArrivalTime = Calendar.getInstance();
                String arrivalPlatform = "";
                String route = "";
                String headsign = "";
                int i = 0;
                boolean error = false;
                while (i != 12) {
                    while (!matcher.find()) {
                        matcher.reset(br.readLine());
                    }
                    String pred = matcher.group().replaceAll("^\"|\"$|^<|>$","");
                    while (!matcher.find()) {
                        matcher.reset(br.readLine());
                    }
                    if (pred.equals(rplod + "stop")) {
                        departureStationId = matcher.group().replaceAll("^\"|\"$|^<|>$","");
                        i++;
                    }
                    else if (pred.equals(rplod + "scheduledDepartureTime")) {
                        try {
                            Date d = sdf.parse(matcher.group().replaceAll("^\"|\"$|^<|>$",""));
                            scheduledDepartureTime.setTime(d);
                        } catch (ParseException ex) {
                            Logger.getLogger(RPLODServerDAO.class.getName()).log(Level.SEVERE,null,ex);
                            error = true;
                        }
                        i++;
                    }
                    else if (pred.equals(rplod + "delay")) {
                        String delay = matcher.group().replaceAll("^\"|\"$|^<|>$","");
                        try {
                            departureDelay = Integer.parseInt(delay);
                        } catch(NumberFormatException e) {
                            if (delay.equals("CANCELLED")) {
                                error = true;
                            }
                        }
                        i++;
                    }
                    else if (pred.equals(rplod + "actualDepartureTime")) {
                        try {
                            Date d = sdf.parse(matcher.group().replaceAll("^\"|\"$|^<|>$",""));
                            actualDepartureTime.setTime(d);
                        } catch (ParseException ex) {
                            Logger.getLogger(RPLODServerDAO.class.getName()).log(Level.SEVERE,null,ex);
                            error = true;
                        }
                        i++;
                    }
                    else if (pred.equals(rplod + "platform")) {
                        departurePlatform = matcher.group().replaceAll("^\"|\"$|^<|>$","");
                        i++;
                    }
                    else if (pred.equals(rplod + "nextStop")) {
                        arrivalStationId = matcher.group().replaceAll("^\"|\"$|^<|>$","");
                        i++;
                    }
                    else if (pred.equals(rplod + "nextStopScheduledArrivalTime")) {
                        try {
                            Date d = sdf.parse(matcher.group().replaceAll("^\"|\"$|^<|>$",""));
                            scheduledArrivalTime.setTime(d);
                        } catch (ParseException ex) {
                            Logger.getLogger(RPLODServerDAO.class.getName()).log(Level.SEVERE,null,ex);
                            error = true;
                        }
                        i++;
                    }
                    else if (pred.equals(rplod + "nextStopDelay")) {
                        String delay = matcher.group().replaceAll("^\"|\"$|^<|>$","");
                        try {
                            arrivalDelay = Integer.parseInt(delay);
                        } catch(NumberFormatException e) {
                            if (delay.equals("CANCELLED")) {
                                error = true;
                            }
                        }
                        i++;
                    }
                    else if (pred.equals(rplod + "nextStopActualArrivalTime")) {
                        try {
                            Date d = sdf.parse(matcher.group().replaceAll("^\"|\"$|^<|>$",""));
                            actualArrivalTime.setTime(d);
                        } catch (ParseException ex) {
                            Logger.getLogger(RPLODServerDAO.class.getName()).log(Level.SEVERE,null,ex);
                            error = true;
                        }
                        i++;
                    }
                    else if (pred.equals(rplod + "nextStopPlatform")) {
                        arrivalPlatform = matcher.group().replaceAll("^\"|\"$|^<|>$","");
                        i++;
                    }
                    else if (pred.equals(rplod + "routeLabel")) {
                        route = matcher.group().replaceAll("^\"|\"$|^<|>$","");
                        i++;
                    }
                    else if (pred.equals(transit + "headsign")) {
                        headsign = matcher.group().replaceAll("^\"|\"$|^<|>$","");
                        i++;
                    }
                    while (!matcher.find()) {
                        matcher.reset(br.readLine());
                    }
                }
                while (!matcher.group().equals(".")) {
                    while (!matcher.find()) {
                        matcher.reset(br.readLine());
                    }
                }
                if (!error && !actualDepartureTime.before(from) && !actualDepartureTime.after(till)) {
                    Connection connection = new Connection(departureStationId,scheduledDepartureTime,
                            departureDelay,actualDepartureTime,departurePlatform,
                            arrivalStationId,scheduledArrivalTime,arrivalDelay,actualArrivalTime,
                            arrivalPlatform,route,headsign);
                    connections.add(connection);
                }
            }
        }
    }
    
    private void parseLinkedcsvStations(BufferedReader br, HashMap<String,Station> stations) throws IOException {
        int station_id = -1;
        int label = -1;
        int lat = -1;
        int lon = -1;
        String[] header = br.readLine().split(csvRegex);
        for (int i = 0; i < header.length; i++) {
            String h = header[i].replaceAll("^\"|\"$","");
            if (h.equals("$id")) {
                station_id = i;
            }
            else if (h.equals("http://www.w3.org/2000/01/rdf-schema#label")) {
                label = i;
            }
            else if (h.equals("http://www.w3.org/2003/01/geo/wgs84_pos#lat")) {
                lat = i;
            }
            else if (h.equals("http://www.w3.org/2003/01/geo/wgs84_pos#long")) {
                lon = i;
            }
        }
        String line;
        while ((line = br.readLine()) != null) {
            String[] data = line.split(csvRegex);
            String id = data[station_id].replaceAll("^\"|\"$","");
            String stationName = data[label].replaceAll("^\"|\"$","");
            double latitude = Double.parseDouble(data[lat].replaceAll("^\"|\"$",""));
            double longitude = Double.parseDouble(data[lon].replaceAll("^\"|\"$",""));
            Station station = new Station(id,stationName,latitude,longitude);
            stations.put(station.getId(),station);
        }
    }
    
    private void parseLinkedcsvDepartures(BufferedReader br, List<Connection> connections, Calendar from, Calendar till) throws IOException {
        int stop = -1;
        int sdt = -1;
        int delay = -1;
        int adt = -1;
        int platform = -1;
        int nextStop = -1;
        int nextStopScheduledArrivalTime = -1;
        int nextStopDelay = -1;
        int nextStopActualArrivalTime = -1;
        int nextStopPlatform = -1;
        int routeLabel = -1;
        int hs = -1;
        String[] header = br.readLine().split(csvRegex);
        for (int i = 0; i < header.length; i++) {
            String h = header[i].replaceAll("^\"|\"$","");
            if (h.equals("http://semweb.mmlab.be/ns/rplod/stop")) {
                stop = i;
            }
            else if (h.equals("http://semweb.mmlab.be/ns/rplod/scheduledDepartureTime")) {
                sdt = i;
            }
            else if (h.equals("http://semweb.mmlab.be/ns/rplod/delay")) {
                delay = i;
            }
            else if (h.equals("http://semweb.mmlab.be/ns/rplod/actualDepartureTime")) {
                adt = i;
            }
            else if (h.equals("http://semweb.mmlab.be/ns/rplod/platform")) {
                platform = i;
            }
            else if (h.equals("http://semweb.mmlab.be/ns/rplod/nextStop")) {
                nextStop = i;
            }
            else if (h.equals("http://semweb.mmlab.be/ns/rplod/nextStopScheduledArrivalTime")) {
                nextStopScheduledArrivalTime = i;
            }
            else if (h.equals("http://semweb.mmlab.be/ns/rplod/nextStopDelay")) {
                nextStopDelay = i;
            }
            else if (h.equals("http://semweb.mmlab.be/ns/rplod/nextStopActualArrivalTime")) {
                nextStopActualArrivalTime = i;
            }
            else if (h.equals("http://semweb.mmlab.be/ns/rplod/nextStopPlatform")) {
                nextStopPlatform = i;
            }
            else if (h.equals("http://semweb.mmlab.be/ns/rplod/routeLabel")) {
                routeLabel = i;
            }
            else if (h.equals("http://vocab.org/transit/terms/headsign")) {
                hs = i;
            }
        }
        String line;
        while ((line = br.readLine()) != null) {
            boolean error = false;
            String[] data = line.split(csvRegex);
            String departureStationId = data[stop].replaceAll("^\"|\"$","");
            Calendar scheduledDepartureTime = Calendar.getInstance();
            int departureDelay = 0;
            Calendar actualDepartureTime = Calendar.getInstance();
            String departurePlatform = data[platform].replaceAll("^\"|\"$","");
            String arrivalStationId = data[nextStop].replaceAll("^\"|\"$","");
            Calendar scheduledArrivalTime = Calendar.getInstance();
            int arrivalDelay = 0;
            Calendar actualArrivalTime = Calendar.getInstance();
            String arrivalPlatform = data[nextStopPlatform].replaceAll("^\"|\"$","");
            String route = data[routeLabel].replaceAll("^\"|\"$","");
            String headsign = data[hs].replaceAll("^\"|\"$","");
            try {
                scheduledDepartureTime.setTime(sdf.parse(data[sdt].replaceAll("^\"|\"$","")));
                actualDepartureTime.setTime(sdf.parse(data[adt].replaceAll("^\"|\"$","")));
                scheduledArrivalTime.setTime(sdf.parse(data[nextStopScheduledArrivalTime].replaceAll("^\"|\"$","")));
                actualArrivalTime.setTime(sdf.parse(data[nextStopActualArrivalTime].replaceAll("^\"|\"$","")));
            } catch (ParseException ex) {
                Logger.getLogger(RPLODServerDAO.class.getName()).log(Level.SEVERE,null,ex);
                error = true;
            }
            try {
                departureDelay = Integer.parseInt(data[delay].replaceAll("^\"|\"$",""));
                arrivalDelay = Integer.parseInt(data[nextStopDelay].replaceAll("^\"|\"$",""));
            } catch(NumberFormatException e) {
                if (data[delay].replaceAll("^\"|\"$","").equals("CANCELLED")
                        || data[nextStopDelay].replaceAll("^\"|\"$","").equals("CANCELLED")) {
                    error = true;
                }
            }
            if (!error && !actualDepartureTime.before(from) && !actualDepartureTime.after(till)) {
                Connection connection = new Connection(departureStationId,scheduledDepartureTime,
                        departureDelay,actualDepartureTime,departurePlatform,
                        arrivalStationId,scheduledArrivalTime,arrivalDelay,actualArrivalTime,
                        arrivalPlatform,route,headsign);
                connections.add(connection);
            }
        }
    }

    public String getName() {
        return name;
    }
    
}