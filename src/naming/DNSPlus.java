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
    AbstractBroker broker = new BrokerWithBalancedTree("Broker1", heps);
    //AbstractBroker broker = new Broker("Broker1", heps);
    
    File serviceNames;
    
    double addSubscriptions = 0;
    Map<String, Double> matchTimings = new HashMap<>();
        
       
    
    public DNSPlus(String file)
    {
        serviceNames = new File(file);
        
        subscriber.setHeps(heps);
        publisher.setHeps(heps);
        
        subscriber.getSecurityParameters();
        publisher.getSecurityParameters();
        
        setParameters();
    }
    
    private void setParameters() 
    {
        matchTimings.put("google.com", 0d);
        matchTimings.put("doesnotexist", 0d);
        matchTimings.put("allmusic.com", 0d);
        matchTimings.put("ticketmaster.com", 0d);
        matchTimings.put("eventbrite.co.uk", 0d);
        matchTimings.put("facebook.com", 0d);
    }
    
    public void generateSubscriptions() throws FileNotFoundException 
    {
        long t;
        
        try (Scanner scanner = new Scanner(serviceNames)) {
            while (scanner.hasNextLine())
            {
                String service = scanner.nextLine();
                //System.out.println(service);
                Subscription s = subscriber.generateSubscription(service);
                t = System.nanoTime();
                broker.addSubscription(s);
                addSubscriptions += (System.nanoTime() - t);
            }
        }
    }
    
    public boolean match(String service, Publication p) 
    {
        double timing = matchTimings.get(service);
        
        long t = System.nanoTime();
        boolean matchResult = broker.matchPublication(p);
        timing += (System.nanoTime() - t);
        
        matchTimings.put(service, timing);
        return matchResult;
    }
    
    
    public static void main(String[] args) 
    {    
        int iterations = 1000;
        double subscriptions = 0;
             
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

        for (String service : dnsPlus.matchTimings.keySet())
        {
            Publication publication = dnsPlus.publisher.generatePublication(service);

            for (int i=0; i<iterations; i++) 
            {
                dnsPlus.match(service, publication);
            }
        }
        
        
        System.out.println("Total Subscriptions table generation (ms) [includes subscriptions generation]: " + subscriptions / 1000000);
        System.out.println("Subscriptions table generation (ms) [actual time to add subscriptions to the table]: " + dnsPlus.addSubscriptions / 1000000);
        
        for (String service : dnsPlus.matchTimings.keySet()) 
        {
         System.out.println("Match [" + service + "] (ms): " + (dnsPlus.matchTimings.get(service) / 1000000) / iterations);   
        }
    } 
}