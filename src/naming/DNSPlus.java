package naming;

import java.io.File;
import java.io.FileNotFoundException;
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
        double subscriptions = 0, match1 = 0, match2 = 0, match3 = 0, match4 = 0;
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
            t = System.nanoTime();
            dnsPlus.match("google.com");
            match1 += System.nanoTime() - t;
            
            t = System.nanoTime();
            dnsPlus.match("doesnotexist");
            match2 += System.nanoTime() - t;
            
            t = System.nanoTime();
            dnsPlus.match("allmusic.com");
            match3 += System.nanoTime() - t;
            
            t = System.nanoTime();
            dnsPlus.match("ticketmaster.com");
            match4 += System.nanoTime() - t;
            
            
        }
        
        System.out.println("Subscriptions table generation (ms): " + subscriptions / 1000000);
        System.out.println("Match 1 [google.com] (ms): " + (match1 / 1000000) / iterations);
        System.out.println("Match 2 [doesnotexist] (ms): " + (match2 / 1000000) / iterations);
        System.out.println("Match 3 [allmusic.com] (ms): " + (match3 / 1000000) / iterations);
        System.out.println("Match 4 [ticketmaster.com] (ms): " + (match4 / 1000000) / iterations);
    } 
}