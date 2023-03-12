package tree;

import java.util.Comparator;
import naming.AbstractBroker;
import naming.Subscription;

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
        return broker.cover(s1, s2);   
    }
    
}
