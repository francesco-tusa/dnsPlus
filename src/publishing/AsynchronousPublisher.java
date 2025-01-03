package publishing;

import encryption.HEPS;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import subscribing.Subscription;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.CustomLogger;
import broker.AsynchronousBroker;
import subscribing.PoisonPillSubscription;


public class AsynchronousPublisher {
    
    private static final Logger logger = CustomLogger.getLogger(AsynchronousPublisher.class.getName());
    private Publisher publisher;
    private AsynchronousBroker broker;
    
    private Map<String, Publication> publications;
    

    public AsynchronousPublisher(String name, AsynchronousBroker broker) {
        this.publisher = new Publisher(name);
        this.broker = broker;
        publications = new HashMap<>();
    }
    
    public void init() {
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
    
    
    public List<Publication> generatePublications(List<String> serviceNames) {
        logger.log(Level.INFO, "Generating Publications...");
        List<Publication> publicationsList = new ArrayList<>();
        for (String service : serviceNames) {
            Publication p = generatePublication(service);
            publications.put(service, p);
            publicationsList.add(p);
        }
        return publicationsList;
    }
    
    public void publish(String service) {
        logger.log(Level.INFO, "Publication for service: {0}", service);
        Publication p = generatePublication(service);
        broker.processPublication(p);
    }
    
    public void publish(Publication p) {
        logger.log(Level.INFO, "Publication for service: {0}", p.getServiceName());
        broker.processPublication(p);
    }
    
    public void generateAndPublishAll(List<String> serviceNames) {
        logger.log(Level.INFO, "Generating Publications...");
        for (String service : serviceNames) {
            if (!publications.containsKey(service)) {
                publications.put(service, publisher.generatePublication(service));
            }
        }
        
        // now sending publications
        logger.log(Level.INFO, "Sending Publications to Broker...");
        for (String service : serviceNames) {
            logger.log(Level.INFO, "Publication for service: {0}", service);
            broker.processPublication(publications.get(service));
        }
    }
    
    public void publishAll(List<Publication> publicationsList) {
        logger.log(Level.INFO, "Sending Publications to Broker...");
        for (Publication p : publicationsList) {
            logger.log(Level.INFO, "Publication for service: {0}", p.getServiceName());
            broker.processPublication(p);
        }
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
            if (s instanceof PoisonPillSubscription) {
                logger.log(Level.WARNING, "publisherReceiver thread being stopped");
                break;
            }
            logger.log(Level.INFO, "The Broker Found a Subscription matching {0}", s.getServiceName());
        }
    }
}