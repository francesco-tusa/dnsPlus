package broker;

import publishing.Publication;
import subscribing.Subscription;

public interface CachingBroker {
    // look up for a publication that matches the given subscription
    // inside the cache
    Publication cacheLookUp(Subscription s); 
}