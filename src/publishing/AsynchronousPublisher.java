package publishing;

import broker.AsynchronousCachingBroker;
import encryption.HEPS;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import subscribing.Subscription;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.CustomLogger;


public class AsynchronousPublisher {
    
    private static final Logger logger = CustomLogger.getLogger(AsynchronousPublisher.class.getName());
    private Publisher publisher;
    private AsynchronousCachingBroker broker;
    
    private Map<String, Publication> publications;
    

    public AsynchronousPublisher(String name, AsynchronousCachingBroker broker) {
        this.publisher = new Publisher(name);
        this.broker = broker;
        publications = new HashMap<>();
    }
    
    public void initialise() {
        publisher.setHeps(HEPS.getInstance());
        publisher.getSecurityParameters();
        receiveSubscriptions();
    }
    
    
    private Publication generatePublication(String service) {
        Publication p;
        if (publications.containsKey(service)) {
            p = publications.get(service);
        } else {
            p = publisher.generatePublication(service);
            publications.put(service, p);
        }
        return p;
    }
    
    public void publish(String service) {
        logger.log(Level.FINE, "Publication for service: {0}", service);
        Publication p = generatePublication(service);
        broker.processPublication(p);
    }
    
    public void publishAll(List<String> serviceNames) {
        logger.log(Level.INFO, "Generating Publications...");
        for (String service : serviceNames) {
            if (!publications.containsKey(service)) {
                publications.put(service, publisher.generatePublication(service));
            }
        }
        
        // now sending publications
        logger.log(Level.INFO, "Sending Publications to Broker...");
        for (String service : serviceNames) {
            broker.processPublication(publications.get(service));
        }
    }
    
    // randomly publish a publication from the list
    public void publish(List<String> serviceNames) {
        int randomIndex = (new Random()).nextInt(0, serviceNames.size());
        publish(serviceNames.get(randomIndex));
    }
    
    
    // randomly publish n publications from the list serviceNames
    public void publish(List<String> serviceNames, int n) {
        int[] randomIndexes = new int[n];
        
        for (int i=0; i<n; i++) {
            randomIndexes[i] = (new Random()).nextInt(0, serviceNames.size());
            generatePublication(serviceNames.get(randomIndexes[i]));
        }
        
        long t = System.nanoTime();
        for (int i=0; i<n; i++) {
            String serviceName = serviceNames.get(randomIndexes[i]);
            broker.processPublication(publications.get(serviceName));
        }
        System.err.println("********** It took " + (System.nanoTime() - t)/1000000 + " msec to process " + n + " publications on a broker **********");
    }
    
    
    // for debug
    private void receiveSubscriptions() {
        Thread publisherReceiverThread = new Thread(() -> listenForSubscriptions());
        publisherReceiverThread.start();
    }
    
    private void listenForSubscriptions() {
        logger.log(Level.INFO, "*** Waiting for results (subscriptions that matched sent publications) ***");
        while (true) {
            Subscription s = broker.getPublicationResult();
            logger.log(Level.INFO, "Found Matching Subscription {0}", s.getServiceName());
        }
    }
}