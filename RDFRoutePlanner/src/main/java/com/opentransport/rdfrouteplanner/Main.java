package com.opentransport.rdfrouteplanner;

import com.opentransport.rdfrouteplanner.containers.Endpoints;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nicola De Clercq
 */
public class Main {
    
    public static void main(String[] args) {
//        BasicCSA1 csa1 = new BasicCSA1();
//        BasicCSA2 csa2 = new BasicCSA2();
//        BasicCSA3 csa3 = new BasicCSA3();
//        BasicCSA4_1 csa4_1 = new BasicCSA4_1();
//        BasicCSA4_2 csa4_2 = new BasicCSA4_2();
//        BasicCSA5 csa5 = new BasicCSA5(Calendar.getInstance());
//        BasicCSA6 csa = new BasicCSA6(6,1000);
        
//        Calendar c = new GregorianCalendar(2014,3,12,18,49);
//        Calendar c = Calendar.getInstance();
//        String nmbs = " [NMBS/SNCB]";
//        String from = "oostende" + nmbs;
//        String to = "sint-niklaas" + nmbs;
//        csa1.calculateRoute(from,to,c);
//        csa2.calculateRoute(from,to,c,5);
//        csa3.calculateRoute(from,to,c,5);
//        csa4_1.calculateRoute(from,to,c,5);
//        csa4_2.calculateRoute(from,to,c,5);
//        csa5.calculateRoute(from,to,c,5);
//        csa5.calculateRoute(to,"aarlen" + nmbs,c,5);
//        csa5.shutDown();
        
        long start = System.currentTimeMillis();
        start();
        long stop = System.currentTimeMillis();
        System.out.println("Time: " + (stop - start) + "ms");
    }
    
    private static void start() {
        System.out.println("Initializing...");
        Properties prop = new Properties();
        File f = new File("config.properties");
        if (f.exists() && f.isFile()) {
            try {
                FileInputStream fis = new FileInputStream(f);
                prop.load(fis);
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE,null,ex);
                System.out.println("Error reading config file: \"" + f.getName() + "\"");
                System.exit(-1);
            }
        }
        else {
            System.out.println("Could not find config file: \"" + f.getName() + "\"");
            System.exit(-2);
        }
        final int hours = Integer.parseInt(prop.getProperty("hours"));
        final int walkingDistance = Integer.parseInt(prop.getProperty("walkingDistance"));
        final int updateInterval = Integer.parseInt(prop.getProperty("updateInterval"));
        BasicCSAFinal csa = new BasicCSAFinal(hours,walkingDistance,updateInterval);
        boolean shutdown = false;
        
        while(!shutdown) {
            System.out.print("Calculate route? ");
            Scanner s = new Scanner(new InputStreamReader(System.in));
            String yes = s.nextLine().toLowerCase();
            if (yes.equals("yes") || yes.equals("y")) {
                System.out.print("Departure station: ");
                String departureStation = s.nextLine();
                System.out.print("Arrival station: ");
                String arrivalStation = s.nextLine();
                Endpoints endpoints = csa.lookUpEndpoints(departureStation,arrivalStation);
                System.out.print("Departure time (ddmmyyhhmm): ");
                SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyHHmm");
                Calendar c = Calendar.getInstance();
                try {
                    c.setTime(sdf.parse(s.nextLine()));
                } catch (ParseException ex) {
                    System.out.println("Unable to parse departure time. Using current time.");
                }
                System.out.print("Minimum change time in minutes: ");
                try {
                    csa.calculateRoute(endpoints,c,Integer.parseInt(s.nextLine()));
                } catch (NumberFormatException ex) {
                    csa.calculateRoute(endpoints,c,5);
                }
            }
            else {
                csa.shutDown();
                s.close();
                shutdown = true;
            }
        }
    }
    
}