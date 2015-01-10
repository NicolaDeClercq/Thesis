package com.opentransport.rdfrouteplanner.dao;

import com.opentransport.rdfrouteplanner.containers.Connection;
import com.opentransport.rdfrouteplanner.containers.Station;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nicola De Clercq
 */
public class SPARQLDAO {
    
    private final String sparqlEndpointURL;
    private final String graphURI;
    private final String name;
    private final SimpleDateFormat sdf;

    public SPARQLDAO(String sparqlEndpointURL, String graphURI, String name) {
        this.sparqlEndpointURL = sparqlEndpointURL;
        this.graphURI = graphURI;
        this.name = name;
        this.sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    }
    
    public void loadStationData(HashMap<String,Station> stations) {
        String query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
                + "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> "
                + "SELECT ?id, ?name, ?latitude, ?longitude WHERE { "
                + "?id rdfs:label ?name ; geo:lat ?latitude ; geo:long ?longitude }";
        try {
            query = URLEncoder.encode(query,"UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(SPARQLDAO.class.getName()).log(Level.SEVERE,null,ex);
        }
        String link = sparqlEndpointURL + "?default-graph-uri=" + graphURI
                + "&query=" + query + "&format=text/csv&timeout=0&debug=on";
        URL url = null;
        try {
            url = new URL(link);
        } catch (MalformedURLException ex) {
            Logger.getLogger(SPARQLDAO.class.getName()).log(Level.SEVERE,null,ex);
        }
        
        BufferedReader br;
        String line;
        try {
            br = new BufferedReader(new InputStreamReader(url.openStream()));
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] data = line.substring(1,line.length() - 1).split("\",\"");
                double latitude = Double.parseDouble(data[2]);
                double longitude = Double.parseDouble(data[3]);
                Station station = new Station(data[0],data[1],latitude,longitude);
                stations.put(station.getId(),station);
            }
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(SPARQLDAO.class.getName()).log(Level.SEVERE,null,ex);
        }
    }

    public void loadConnectionData(List<Connection> connections, Calendar from, Calendar to, int limit, int offset) {
        String query = "PREFIX rplod: <http://semweb.mmlab.be/ns/rplod/> "
                + "PREFIX transit: <http://vocab.org/transit/terms/> "
                + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
                + "SELECT ?departureStationId, ?scheduledDepartureTime, "
                + "?departureDelay, ?actualDepartureTime, ?departurePlatform, "
                + "?arrivalStationId, ?scheduledArrivalTime, ?arrivalDelay, "
                + "?actualArrivalTime, ?arrivalPlatform, ?route, ?headsign "
                + "WHERE { ?departure rplod:stop ?departureStationId ; "
                + "rplod:scheduledDepartureTime ?scheduledDepartureTime ; "
                + "rplod:delay ?departureDelay ; rplod:actualDepartureTime "
                + "?actualDepartureTime ; rplod:platform ?departurePlatform ; "
                + "transit:headsign ?headsign ; rplod:routeLabel ?route ; "
                + "rplod:nextStop ?arrivalStationId ; rplod:nextStopScheduledArrivalTime "
                + "?scheduledArrivalTime ; rplod:nextStopDelay ?arrivalDelay ; "
                + "rplod:nextStopActualArrivalTime ?actualArrivalTime ; "
                + "rplod:nextStopPlatform ?arrivalPlatform ";
        if (from != null) {
            query += "FILTER (xsd:dateTime(?actualDepartureTime) > xsd:dateTime('";
            query += sdf.format(from.getTime());
            query += "')";
            if (to != null) {
                query += " && xsd:dateTime(?actualDepartureTime) <= xsd:dateTime('";
                query += sdf.format(to.getTime());
                query += "')";
            }
            query += ") ";
        }
        query += "} ";
        query += "ORDER BY ASC(xsd:dateTime(?actualDepartureTime)) ";
        if (limit > 0) {
            query += "LIMIT " + limit + " ";
            if (offset > 0) {
                query += "OFFSET " + offset;
            }
        }
        
        try {
            query = URLEncoder.encode(query,"UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(SPARQLDAO.class.getName()).log(Level.SEVERE,null,ex);
        }
        String link = sparqlEndpointURL + "?default-graph-uri=" + graphURI
                + "&query=" + query + "&format=text/csv&timeout=0&debug=on";
        URL url = null;
        try {
            url = new URL(link);
        } catch (MalformedURLException ex) {
            Logger.getLogger(SPARQLDAO.class.getName()).log(Level.SEVERE,null,ex);
        }
        
        BufferedReader br;
        String line;
        try {
            br = new BufferedReader(new InputStreamReader(url.openStream()));
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] data = line.substring(1,line.length() - 1).split("\",\"");
                Calendar scheduledDepartureTime = Calendar.getInstance();
                Calendar actualDepartureTime = Calendar.getInstance();
                Calendar scheduledArrivalTime = Calendar.getInstance();
                Calendar actualArrivalTime = Calendar.getInstance();
                try {
                    Date sdt = sdf.parse(data[1]);
                    Date adt = sdf.parse(data[3]);
                    Date sat = sdf.parse(data[6]);
                    Date aat = sdf.parse(data[8]);
                    scheduledDepartureTime.setTime(sdt);
                    actualDepartureTime.setTime(adt);
                    scheduledArrivalTime.setTime(sat);
                    actualArrivalTime.setTime(aat);

                } catch (ParseException ex) {
                    Logger.getLogger(SPARQLDAO.class.getName()).log(Level.SEVERE,null,ex);
                    continue;
                }
                int departureDelay = 0;
                int arrivalDelay = 0;
                try {
                    departureDelay = Integer.parseInt(data[2]);
                    arrivalDelay = Integer.parseInt(data[7]);
                } catch (NumberFormatException ex) {
                    if (data[2].equals("CANCELLED")) {
                        continue;
                    }
                }
                Connection connection = new Connection(data[0],scheduledDepartureTime,
                        departureDelay,actualDepartureTime,data[4],data[5],scheduledArrivalTime,arrivalDelay,actualArrivalTime,
                        data[9],data[10],data[11]);
                connections.add(connection);
            }
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(SPARQLDAO.class.getName()).log(Level.SEVERE,null,ex);
        }
    }

    public String getName() {
        return name;
    }
    
}