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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import publishing.PoisonPillPublication;


public final class AsynchronousSubscriber {
    private static final Logger logger = CustomLogger.getLogger(AsynchronousSubscriber.class.getName());
    private Subscriber subscriber;
    private AsynchronousBroker broker;
    
    private Map<String, Subscription> subscriptions;
    private BlockingQueue<Publication> publicationsQueue;
    
    public AsynchronousSubscriber(String name, AsynchronousBroker broker) {
        this.broker = broker;
        this.subscriber = new Subscriber(name);  
        subscriptions = new HashMap<>();
        publicationsQueue = new LinkedBlockingQueue<>();
    }
    
    public String getName() {
        return subscriber.getName();
    }
    
    public void init() {
        subscriber.setHeps(HEPS.getInstance());
        subscriber.getSecurityParameters();
        register();
        receivePublications();
    }
    
    private void register() {
        broker.register(this);
    }
    
    private Subscription generateSubscription(String service) {
        Subscription s;
        if (subscriptions.containsKey(service)) {
            s = subscriptions.get(service);
        } else {
            s = subscriber.generateSubscription(service);
            s.addSubscriber(getName());
            subscriptions.put(service, s);
        }
        return s;
    }
    
    public List<Subscription> generateSubscriptions(List<String> serviceNames) {
        logger.log(Level.INFO, "Subscriber {0} is generating Subscriptions...", getName());
        List<Subscription> subscriptionsList = new ArrayList<>();
        for (String service : serviceNames) {
            Subscription s = generateSubscription(service);
            s.addSubscriber(getName());
            subscriptions.put(service, s);
            subscriptionsList.add(s);
        }
        return subscriptionsList;
    }
    
    public void subscribe(String service) {
        logger.log(Level.INFO, "Subscriber {0} is subscribing to service: {1}", new Object[]{getName(), service});
        Subscription s = generateSubscription(service);
        s.addSubscriber(getName());
        broker.processSubscription(s);
    }
    
    public void subscribe(Subscription s) {
        logger.log(Level.INFO, "Subscriber {0} is subscribing to service: {1}", new Object[]{getName(), s.getServiceName()});
        broker.processSubscription(s);
    }
    
    
    public void generateAndSubscribeToAll(List<String> serviceNames) {
        logger.log(Level.INFO, "Subscriber {0} is generating Subscriptions...", getName());
        for (String service : serviceNames) {
            if (!subscriptions.containsKey(service)) {
                Subscription s = subscriber.generateSubscription(service);
                s.addSubscriber(getName());
                subscriptions.put(service, s);
            }
        }
        
        // now sending subscriptions
        logger.log(Level.INFO, "Subscriber {0} is sending Subscriptions to Broker...", getName());
        for (String service : serviceNames) {
            logger.log(Level.INFO, "Subscriber {0} is subscribing to service: {1}", new Object[]{getName(), service});
            broker.processSubscription(subscriptions.get(service));
        }
    }
    
    
    public void subscribeToAll(List<Subscription> subscriptionsList) {
         logger.log(Level.INFO, "Subscriber {0} is sending Subscriptions to Broker...", getName());
        for (Subscription s : subscriptionsList) {
            logger.log(Level.INFO, "Subscriber {0} is subscribing to service: {1}", new Object[]{getName(), s.getServiceName()});
            broker.processSubscription(s);
        }
    }
    
    public void addMatchingPublication(Publication p) {
        logger.log(Level.WARNING, "Added publication for {0} to the Queue", p.getServiceName());
        publicationsQueue.offer(p);
    }
    
    private void receivePublications() {
        Thread subscriberReceiverThread = new Thread(() -> listenForPublications());
        subscriberReceiverThread.start();
    }
    
    
    private void listenForPublications() {
        logger.log(Level.INFO, "*** Subscriber {0} is waiting for results (publications that matched their subscriptions) ***", getName());
        while (true) {
            try {
                //Publication p = broker.getSubscriptionResult();
                Publication p = publicationsQueue.take();
                if (p instanceof PoisonPillPublication) {
                    logger.log(Level.WARNING, "subscriberReceiver thread for {0} being stopped", getName());
                    break;
                }
                logger.log(Level.INFO, "The Broker Found and Returned a Matching Publication {0} for {1}", new Object[]{p.getServiceName(), getName()});
            } catch (InterruptedException ex) {
                 logger.log(Level.WARNING, "Thread {0} was interrupted",  Thread.currentThread().getName());
            }
        } 
    }
}