package experiments;

import broker.AsynchronousCachingBroker;
import encryption.HEPS;
import broker.tree.binarybalanced.ConcurrentBrokerWithBinaryBalancedTreeAndCache;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import publishing.AsynchronousPublisher;
import subscribing.AsynchronousSubscriber;
import utils.CustomLogger;
import utils.FileLoader;

/**
 *
 * @author f.tusa
 */
public final class ConcurrentDNSWithCacheTest 
{      
    private static final Logger logger = CustomLogger.getLogger(ConcurrentDNSWithCacheTest.class.getName(), Level.INFO);
    private AsynchronousSubscriber subscriber; 
    private AsynchronousPublisher publisher;
    private AsynchronousCachingBroker broker = new ConcurrentBrokerWithBinaryBalancedTreeAndCache("Broker1", HEPS.getInstance());
   
    private String fileName;
    private List<String> serviceNames;
    
    
    public ConcurrentDNSWithCacheTest(String fileName)
    {
        broker = new ConcurrentBrokerWithBinaryBalancedTreeAndCache("Broker1", HEPS.getInstance());
        subscriber = new AsynchronousSubscriber("Subscriber1", broker);
        publisher = new AsynchronousPublisher("Publisher1", broker);
        this.fileName = fileName;
        serviceNames = new ArrayList<>();
    }
    
    
    public void initialise() {
        try {
            serviceNames = FileLoader.loadNames(fileName);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while loading the list of service names from file: {0}", fileName);
            Collections.addAll(serviceNames, "youtube.com", "facebook.com", "wikipedia.org", "reddit.com", "instagram.com",
                                             "tiktok.com", "pinterest.com", "quora.com", "amazon.com", "linkedin.com",
                                             "twitter.com", "google.com", "ebay.com", "apple.com", "etsy.com");
        }
        
        publisher.initialise();
        subscriber.initialise();
    }
    
    
    public void start() {
        Thread publisherThread = new Thread(() -> {
                                                   publisher.publish(serviceNames, 1000);
                                                   publisher.publish(serviceNames, 1000);
                                                   publisher.publish(serviceNames, 1000);
                                                  });
        
        Thread subscriberThread = new Thread(() -> { 
                                                    subscriber.subscribe(serviceNames, 2000);
                                                   });
        publisherThread.start();
        subscriberThread.start();
    }
       
    
    public static void main(String[] args) 
    {
        String home = System.getProperty("user.home");
        ConcurrentDNSWithCacheTest dnsPlus = new ConcurrentDNSWithCacheTest(home + "/websites.txt");     
        
        dnsPlus.initialise();
        dnsPlus.start();
    }
}