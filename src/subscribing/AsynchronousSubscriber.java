package subscribing;

import broker.AsynchronousCachingBroker;
import encryption.HEPS;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import publishing.Publication;


public final class AsynchronousSubscriber {
    private Subscriber subscriber;
    private AsynchronousCachingBroker broker;
    
    private Map<String, Subscription> subscriptions;
    
    
    public AsynchronousSubscriber(String name, AsynchronousCachingBroker broker) {
        this.subscriber = new Subscriber(name);  
        this.broker = broker;
        subscriptions = new HashMap<>();
    }
    
    public void initialise() {
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
    
    public void subscribe(String service) {
        System.out.println("Subscribing to service: " + service);
        Subscription s = generateSubscription(service);
        broker.processSubscription(s);
    }
    
    
    public void subscribeToAll(List<String> serviceNames) {
        System.out.println("Generating Subscriptions...");
        for (String service : serviceNames) {
            if (!subscriptions.containsKey(service)) {
                subscriptions.put(service, subscriber.generateSubscription(service));
            }
        }
        
        // now sending subscriptions
        System.out.println("Sending Subscriptions to Broker...");
        for (String service : serviceNames) {
            broker.processSubscription(subscriptions.get(service));
        }
    }
    
    
    // randomly subscribe to n services from the list serviceNames
    public void subscribe(List<String> serviceNames, int n) {
        int[] randomIndexes = new int[n];
        
        for (int i=0; i<n; i++) {
            randomIndexes[i] = (new Random()).nextInt(0, serviceNames.size());
            generateSubscription(serviceNames.get(randomIndexes[i]));
        }
        
        long t = System.nanoTime();
        for (int i=0; i<n; i++) {
            String serviceName = serviceNames.get(randomIndexes[i]);
            broker.processSubscription(subscriptions.get(serviceName));
        }
        System.err.println("********** It took " + (System.nanoTime() - t)/1000000 + " msec to process " + n + " subscriptions on a broker **********");
    }
    
    
    private void receivePublications() {
        Thread subscriberReceiverThread = new Thread(() -> listenForPublications());
        subscriberReceiverThread.start();
    }
    
    
    private void listenForPublications() {
        System.out.println("*** Waiting for results (publications that matched sent subscriptions) ***");
        while (true) {
            Publication p = broker.getSubscriptionResult();
            System.out.println("*** Matching Publication ---->> " + p.getServiceName());
        }
    }
}