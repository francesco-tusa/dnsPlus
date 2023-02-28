package naming;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 *
 * @author f.tusa
 */
public final class DNSPlus 
{
    // may use a list for each type of entity
    HEPS heps = new HEPS(2048, 2048/8, 512);
        
    Subscriber subscriber = new Subscriber("Subscriber1");
    Publisher publisher = new Publisher("Publisher1");
    Broker broker = new Broker("Broker1", heps);
    
    File serviceNames;
    
    public DNSPlus(String file)
    {
        serviceNames = new File(file);
        
        subscriber.setHeps(heps);
        publisher.setHeps(heps);
        
        subscriber.getSecurityParameters();
        publisher.getSecurityParameters();
    }
    
    public void generateSubscriptions() throws FileNotFoundException 
    {
        try (Scanner scanner = new Scanner(serviceNames)) {
            while (scanner.hasNextLine())
            {
                String service = scanner.nextLine();
                //System.out.println(service);
                broker.addSubscription(subscriber.generateSubscription(service));
            }
        }
    }
    
    public boolean match(String service) 
    {
      Publication p1 = publisher.generatePublication(service);
      return broker.matchPublication(p1);
    }
    
    
    public static void main(String[] args) 
    {    
        int iterations = 100;
        double subscriptions = 0;
        
        Map<String, Double> matchTimings = new HashMap<>();
        
        matchTimings.put("google.com", 0d);
        matchTimings.put("doesnotexist", 0d);
        matchTimings.put("allmusic.com", 0d);
        matchTimings.put("ticketmaster.com", 0d);
        matchTimings.put("eventbrite.co.uk", 0d);
        matchTimings.put("facebook.com", 0d);
             
        long t;
        
        String home = System.getProperty("user.home");
        DNSPlus dnsPlus = new DNSPlus(home + "/websites");

        try {
            System.out.println("Generating subscriptions table");
            t = System.nanoTime();
            dnsPlus.generateSubscriptions(); 
            subscriptions = System.nanoTime() - t;
            
        } catch (FileNotFoundException ex) {
            System.out.println("Subscription could not be loaded: " + ex.getMessage());
            System.exit(-1);
        }
        
        
        System.out.println("Matching publications");
        for (int i=0; i<iterations; i++) 
        {
            double timing;
            for (String service : matchTimings.keySet())
            {
                timing = matchTimings.get(service);
                
                t = System.nanoTime();
                dnsPlus.match(service);
                timing += System.nanoTime() - t;
                
                matchTimings.put(service, timing);
                
            }
        }
        
        System.out.println("Subscriptions table generation (ms): " + subscriptions / 1000000);
        
        for (String service : matchTimings.keySet()) 
        {
         System.out.println("Match [" + service + "] (ms): " + (matchTimings.get(service) / 1000000) / iterations);   
        }
    } 
}