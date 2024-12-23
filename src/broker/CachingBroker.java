package broker;

import publishing.Publication;
import subscribing.Subscription;

public interface CachingBroker {

    boolean checkCacheMatch(Subscription s);
    void cachePublication(Publication p);
    
}
