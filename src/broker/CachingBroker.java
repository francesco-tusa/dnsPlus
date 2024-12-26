package broker;

import publishing.Publication;
import subscribing.Subscription;

public interface CachingBroker {
    // match a subscription with the publication cache content
    Publication matchSubscription(Subscription s); 
}
