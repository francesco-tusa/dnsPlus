package subscribing;

import encryption.HEPS;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import publishing.Publication;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.CustomLogger;
import broker.AsynchronousBroker;
import publishing.PoisonPillPublication;


public final class AsynchronousSubscriber {
    private static final Logger logger = CustomLogger.getLogger(AsynchronousSubscriber.class.getName());
    private Subscriber subscriber;
    private AsynchronousBroker broker;
    
    private Map<String, Subscription> subscriptions;
    
    public AsynchronousSubscriber(String name, AsynchronousBroker broker) {
        this.subscriber = new Subscriber(name);  
        this.broker = broker;
        subscriptions = new HashMap<>();
    }
    
    public void init() {
        subscriber.setHeps(HEPS.getInstance());
        subscriber.getSecurityParameters();
        receivePublications();
    }
    
    
    private Subscription generateSubscription(String service) {
        Subscription s;
        if (subscriptions.containsKey(service)) {
            s = subscriptions.get(service);
        } else {
            s = subscriber.generateSubscription(service);
            subscriptions.put(service, s);
        }
        return s;
    }
    
    public List<Subscription> generateSubscriptions(List<String> serviceNames) {
        logger.log(Level.INFO, "Generating Subscriptions...");
        List<Subscription> subscriptionsList = new ArrayList<>();
        for (String service : serviceNames) {
            Subscription s = generateSubscription(service);
            subscriptions.put(service, s);
            subscriptionsList.add(s);
        }
        return subscriptionsList;
    }
    
    public void subscribe(String service) {
        logger.log(Level.INFO, "Subscribing to service: {0}", service);
        Subscription s = generateSubscription(service);
        broker.processSubscription(s);
    }
    
    public void subscribe(Subscription s) {
        logger.log(Level.INFO, "Subscribing to service: {0}", s.getServiceName());
        broker.processSubscription(s);
    }
    
    
    public void generateAndSubscribeToAll(List<String> serviceNames) {
        logger.log(Level.INFO, "Generating Subscriptions...");
        for (String service : serviceNames) {
            if (!subscriptions.containsKey(service)) {
                subscriptions.put(service, subscriber.generateSubscription(service));
            }
        }
        
        // now sending subscriptions
        logger.log(Level.INFO, "Sending Subscriptions to Broker...");
        for (String service : serviceNames) {
            logger.log(Level.INFO, "Subscribing to service: {0}", service);
            broker.processSubscription(subscriptions.get(service));
        }
    }
    
    
    public void subscribeToAll(List<Subscription> subscriptionsList) {
        logger.log(Level.INFO, "Sending Subscriptions to Broker...");
        for (Subscription s : subscriptionsList) {
            logger.log(Level.INFO, "Subscribing to service: {0}", s.getServiceName());
            broker.processSubscription(s);
        }
    }
    
    
    
    private void receivePublications() {
        Thread subscriberReceiverThread = new Thread(() -> listenForPublications());
        subscriberReceiverThread.start();
    }
    
    
    private void listenForPublications() {
        logger.log(Level.INFO, "*** Waiting for results (publications that matched sent subscriptions) ***");
        while (true) {
            Publication p = broker.getSubscriptionResult();
            if (p instanceof PoisonPillPublication) {
                logger.log(Level.WARNING, "subscriberReceiver thread being stopped");
                break;
            }
            logger.log(Level.INFO, "The Broker Found and Returned a Matching Publication {0}", p.getServiceName());
        } 
    }
}