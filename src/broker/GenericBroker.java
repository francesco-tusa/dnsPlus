package broker;

public interface GenericBroker<S, P> {
    
    void addSubscription(S s);

    S matchPublication(P p);
    
    void processPublication(P p);

    void processSubscription(S s);
}