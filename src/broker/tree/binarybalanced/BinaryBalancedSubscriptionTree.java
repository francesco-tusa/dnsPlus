package broker.tree.binarybalanced;

import broker.SubscriptionComparator;
import java.util.Map;
import java.util.TreeMap;
import encryption.BlindedMatchingBroker;
import broker.tree.SubscriptionTree;
import java.util.Collections;
import java.util.List;
import publishing.Publication;
import subscribing.Subscription;

/**
 *
 * @author uceeftu
 */
public class BinaryBalancedSubscriptionTree implements SubscriptionTree 
{    
    BlindedMatchingBroker broker;

    Map<Subscription, Subscription> tree;

    public BinaryBalancedSubscriptionTree(BlindedMatchingBroker b) 
    {
        broker = b;
        tree = Collections.synchronizedMap(new TreeMap<>(new SubscriptionComparator(broker)));
    }    
    
    @Override
    public Subscription addNode(Subscription s) 
    {
        Subscription found = tree.get(s);
        if (found != null) {
            // the list of subscribers in a new subscription 's' always has
            // one element before this check is performed
            List<String> subscribers = found.getSubscribers();
            String newSubscriber = s.getSubscribers().getFirst();
            if (!subscribers.contains(newSubscriber))  
                found.addSubscriber(newSubscriber);
            
            return found;
        }
        return tree.put(s, s);
    }
    
    
    @Override
    public Subscription search(Publication p)
    {
        // a subscription is created from the publication for searching
        // return null if there is no match
        return tree.get(new Subscription(p.getMatchValue(), p.getServiceName()));
    }
}
