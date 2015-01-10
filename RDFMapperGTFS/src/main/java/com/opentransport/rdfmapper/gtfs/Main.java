package com.opentransport.rdfmapper.gtfs;

import com.opentransport.rdfmapper.gtfs.containers.MapperInput;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Calendar;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.simpleframework.http.core.ContainerSocketProcessor;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

/**
 *
 * @author Nicola De Clercq
 */
public class Main {
    
    public static void main(String[] args) {
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
        
        final String agency = prop.getProperty("agency");
        final String date = prop.getProperty("date");
        final int hours = Integer.parseInt(prop.getProperty("hours"));
        final int port = Integer.parseInt(prop.getProperty("port"));
        final int updateInterval = Integer.parseInt(prop.getProperty("updateInterval"));
        final boolean stoppable = Boolean.parseBoolean(prop.getProperty("stoppable"));
        
        Fetcher fetcher = new Fetcher(agency);
        final MapperInput mi = fetcher.fetch(date);
        
//        Trip t = mi.getTrips().get(0);
//        System.out.println(t.getRoute());
//        System.out.println(t.getHeadsign());
//        List<StopTime> l = new ArrayList(t.getStops().values());
//        for (int i = 0; i < l.size(); i++) {
//            System.out.println(l.get(i));
//        }
        
        Calendar startTime = Calendar.getInstance();
//        GregorianCalendar startTime = new GregorianCalendar(2014,Calendar.OCTOBER,8,0,0);
        
//        JenaMapper.map(mi,startTime,hours);
        
        SortedMapper mapper = new SortedMapper(mi,startTime,hours);
        
        final ServerContainer container = new ServerContainer(mapper,agency);
        SocketAddress address = new InetSocketAddress(port);
        try {
            ContainerSocketProcessor csp = new ContainerSocketProcessor(container);
            Connection connection = new SocketConnection(csp);
            connection.connect(address);
            System.out.println(Calendar.getInstance().getTime() + ": SERVER STARTED AT PORT " + port);
            
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    System.out.println(Calendar.getInstance().getTime() + ": START OF UPDATE");
                    long start = System.currentTimeMillis();
                    container.setMapper(new SortedMapper(mi,Calendar.getInstance(),hours));
                    long end = System.currentTimeMillis();
                    System.out.println(Calendar.getInstance().getTime() + ": END OF UPDATE (" + (end - start) + " ms)");
                }
            },updateInterval,updateInterval,TimeUnit.SECONDS);
            
            if (stoppable) {
                boolean shutdown = false;
                while(!shutdown) {
                    System.out.println("Type STOP to shut down the server");
                    Scanner s = new Scanner(new InputStreamReader(System.in));
                    if (s.nextLine().toLowerCase().equals("stop")) {
                        scheduler.shutdown();
                        connection.close();
                        s.close();
                        System.out.println(Calendar.getInstance().getTime() + ": SERVER STOPPED!");
                        shutdown = true;
                    }
                    else {
                        System.out.print("ERROR! ");
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE,null,ex);
        }
    }
    
}
