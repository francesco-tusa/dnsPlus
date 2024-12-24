package broker;

import java.util.Comparator;
import subscribing.Subscription;

/**
 *
 * @author uceeftu
 */
public class SubscriptionComparator implements Comparator<Subscription> {
    
    private final AbstractBroker broker;

    public SubscriptionComparator(AbstractBroker b) {
        broker = b;
    }

    @Override
    public int compare(Subscription s1, Subscription s2) 
    {    
        return broker.match(s1, s2);   
    }
}
