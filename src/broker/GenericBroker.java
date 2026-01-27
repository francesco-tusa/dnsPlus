package broker;

import publishing.AbstractPublication;
import subscribing.AbstractSubscription;

public interface GenericBroker<S extends AbstractSubscription, P extends AbstractPublication> {
    void addSubscription(S s);

    S matchPublication(P p);
    
    void processPublication(P p);

    void processSubscription(S s);
}