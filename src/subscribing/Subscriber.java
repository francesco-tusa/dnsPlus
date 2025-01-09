package subscribing;

import encryption.BlindingSubscriber;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.CustomLogger;
import broker.Broker;


public class Subscriber extends BlindingSubscriber {
    private static final Logger logger = CustomLogger.getLogger(Subscriber.class.getName());
    private Broker broker;
    
    private Map<String, Subscription> subscriptions;
    
    
    public Subscriber(String name, Broker broker) {
        super(name);
        this.broker = broker;
        subscriptions = new HashMap<>();
    }
    
    public Subscriber(String name) {
        this(name, null);
    }

    
    public Broker getBroker() {
        return broker;
    }

    public void setBroker(Broker broker) {
        this.broker = broker;
    }
   
    
    @Override
    public Subscription generateSubscription(String service) {
        Subscription s;
        if (subscriptions.containsKey(service)) {
            s = subscriptions.get(service);
        } else {
            s = super.generateSubscription(service);
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
        logger.log(Level.WARNING, "Subscriber {0} is generating Subscriptions...", getName());
        for (String service : serviceNames) {
            if (!subscriptions.containsKey(service)) {
                Subscription s = super.generateSubscription(service);
                s.addSubscriber(getName());
                subscriptions.put(service, s);
            }
        }
        
        // now sending subscriptions
        logger.log(Level.WARNING, "Subscriber {0} is sending Subscriptions to Broker...", getName());
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

}