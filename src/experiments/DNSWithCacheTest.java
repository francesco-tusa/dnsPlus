package experiments;

import encryption.HEPS;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import broker.tree.binarybalanced.BrokerWithBinaryBalancedTreeAndCache;
import publishing.Publication;
import publishing.Publisher;
import subscribing.Subscriber;

/**
 *
 * @author f.tusa
 */
public final class DNSWithCacheTest 
{
    // may use a list for each type of entity
    HEPS heps = HEPS.getInstance();
        
    Subscriber subscriber = new Subscriber("Subscriber1");
    Publisher publisher = new Publisher("Publisher1");
    BrokerWithBinaryBalancedTreeAndCache broker = new BrokerWithBinaryBalancedTreeAndCache("Broker1", heps);
    
    File serviceNames;
    String[] services;
    
    double cachePublications = 0;
    Map<String, Double> matchTimings = new HashMap<>();
        
       
    
    public DNSWithCacheTest(String file)
    {
        serviceNames = new File(file);
        services = new String[1000];
        
        subscriber.setHeps(heps);
        publisher.setHeps(heps);
        
        subscriber.getSecurityParameters();
        publisher.getSecurityParameters();
    }
    
    
    public void simpleTest() {
        System.out.println("Publication: fonts");
        broker.matchPublication(publisher.generatePublication("fonts.googleapis.com"));
     
        System.out.println("Publication: facebook");
        broker.matchPublication(publisher.generatePublication("facebook.com"));
        
        System.out.println("Publication: facebook");
        broker.matchPublication(publisher.generatePublication("facebook.com"));
        
        System.out.println("Subscription facebook");
        broker.matchSubscription(subscriber.generateSubscription("facebook.com"));
        
        System.out.println("Subscription facebook");
        broker.matchSubscription(subscriber.generateSubscription("facebook.com")); 
        
        System.out.println("Publication: facebook");
        broker.matchPublication(publisher.generatePublication("facebook.com"));
    }
    
    public void generatePublications() throws FileNotFoundException 
    {
        long t;
        int i = 0;
        
        try (Scanner scanner = new Scanner(serviceNames)) {
            while (scanner.hasNextLine())
            {
                String service = scanner.nextLine();
                services[i++] = service;
                System.out.println("Publication: " + service);
                Publication p = publisher.generatePublication(service);
                t = System.nanoTime();
                broker.matchPublication(p);
                cachePublications += (System.nanoTime() - t);
            }
        }
    }
    
//    public boolean generateSubscriptions(String service, Publication p) 
//    {
//        double timing = matchTimings.get(service);
//        
//        long t = System.nanoTime();
//        boolean matchResult = broker.matchSubscription(p);
//        timing += (System.nanoTime() - t);
//        
//        matchTimings.put(service, timing);
//        return matchResult;
//    }
//    
//    
//    public static void main(String[] args) 
//    {    
//        int iterations = 1;
//        double subscriptions = 0;
//             
//        long t;
//        
//        String home = System.getProperty("user.home");
//        DNSWithCacheTest dnsPlus = new DNSWithCacheTest(home + "/websites.txt");
//
//        try {
//            System.out.println("*** Generating subscriptions table ***");
//            t = System.nanoTime();
//            dnsPlus.generateSubscriptions(); 
//            subscriptions = System.nanoTime() - t;
//            
//        } catch (FileNotFoundException ex) {
//            System.out.println("Subscription could not be loaded: " + ex.getMessage());
//            System.exit(-1);
//        }
//        
//        System.out.println();
//        System.out.println("*** Matching publications ***");
//
//        for (String service : dnsPlus.matchTimings.keySet())
//        {
//            Publication publication = dnsPlus.publisher.generatePublication(service);
//
//            for (int i=0; i<iterations; i++) 
//            {
//                System.out.println(service + ": " + dnsPlus.match(service, publication));
//            }
//        }
//        
//        System.out.println();
//        System.out.println("Total Subscriptions table generation (ms) [includes subscriptions generation]: " + subscriptions / 1000000);
//        System.out.println("Subscriptions table generation (ms) [actual time to add subscriptions to the table]: " + dnsPlus.cachePublications / 1000000);
//        System.out.println();
//        double timingSum = 0;
//        for (String service : dnsPlus.matchTimings.keySet()) 
//        {
//            double timing = (dnsPlus.matchTimings.get(service) / 1000000) / iterations;
//            System.out.println("Match [" + service + "] (ms): " + String.format("%.2f", timing));
//            timingSum += timing;
//        }
//        
//        System.out.println();
//        System.out.println("Average match time (ms): " + timingSum / dnsPlus.matchTimings.size());
//    } 
    
    
    public static void main(String[] args) 
    {
        String home = System.getProperty("user.home");
        DNSWithCacheTest dnsPlus = new DNSWithCacheTest(home + "/websites.txt");
        
        dnsPlus.simpleTest();
        
    }
}