package publishing;

import encryption.BlindingPublisher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.CustomLogger;
import broker.Broker;


public class Publisher extends BlindingPublisher {
    
    private static final Logger logger = CustomLogger.getLogger(Publisher.class.getName());
    private Broker broker;
    
    private Map<String, Publication> publications;
    

    public Publisher(String name, Broker broker) {
        super(name);
        this.broker = broker;
        publications = new HashMap<>();
    }

    protected Broker getBroker() {
        return broker;
    }
    
    

    
    
    @Override
    public Publication generatePublication(String service) {
        Publication p;
        if (publications.containsKey(service)) {
            p = publications.get(service);
        } else {
            p = super.generatePublication(service);
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
        logger.log(Level.WARNING, "Publisher {0} is generating Publications...", getName());
        for (String service : serviceNames) {
            if (!publications.containsKey(service)) {
                publications.put(service, super.generatePublication(service));
            }
        }
        
        // now sending publications
         logger.log(Level.WARNING, "Publisher {0} is sending Publications to Broker...", getName());
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
}