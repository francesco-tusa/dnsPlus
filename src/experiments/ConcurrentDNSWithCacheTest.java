package experiments;

import broker.AsynchronousCachingBroker;
import encryption.HEPS;
import broker.tree.binarybalanced.ConcurrentBrokerWithBinaryBalancedTreeAndCache;
import experiments.inputdata.DBFactory;
import experiments.inputdata.DomainDB;
import java.util.logging.Level;
import java.util.logging.Logger;
import publishing.AsynchronousPublisher;
import subscribing.AsynchronousSubscriber;
import utils.CustomLogger;

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

    private DomainDB domainDB;
    
    
    public ConcurrentDNSWithCacheTest(String fileName)
    {
        broker = new ConcurrentBrokerWithBinaryBalancedTreeAndCache("Broker1", HEPS.getInstance());
        subscriber = new AsynchronousSubscriber("Subscriber1", broker);
        publisher = new AsynchronousPublisher("Publisher1", broker);
        domainDB = DBFactory.getDomainDB(fileName);
    }
    
    
    public void initialise() {
        publisher.initialise();
        subscriber.initialise();
    }
    
    
    public void start() {
        Thread publisherThread = new Thread(() -> {
                                                   publisher.publishAll(domainDB.getRandomEntries(1000));
                                                   publisher.publishAll(domainDB.getRandomEntries(1000));
                                                   publisher.publishAll(domainDB.getRandomEntries(1000));
                                                  });
        
        Thread subscriberThread = new Thread(() -> { 
                                                    subscriber.subscribeToAll(domainDB.getRandomEntries(2000));
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