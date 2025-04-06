package broker;

import publishing.Publication;
import subscribing.Subscription;

public interface GenericBroker<S extends Subscription, P extends Publication> {
    void addSubscription(S s);

    S matchPublication(P p);
    
    void processPublication(P p);

    void processSubscription(S s);
}