package broker.tree.binarybalanced;

import broker.SubscriptionComparator;
import java.util.Map;
import java.util.TreeMap;
import broker.AbstractBroker;
import broker.tree.SubscriptionTree;
import java.util.Collections;
import publishing.Publication;
import subscribing.Subscription;

/**
 *
 * @author uceeftu
 */
public class BinaryBalancedSubscriptionTree implements SubscriptionTree 
{    
    AbstractBroker broker;

    Map<Subscription, Subscription> tree;

    public BinaryBalancedSubscriptionTree(AbstractBroker b) 
    {
        broker = b;
        tree = Collections.synchronizedMap(new TreeMap<>(new SubscriptionComparator(broker)));
    }    
    
    @Override
    public Subscription addNode(Subscription s) 
    {
        return tree.putIfAbsent(s, s);
    }
    
    
    @Override
    public Subscription search(Publication p)
    {
        // a subscription is created from the publication for searching
        // return null if there is no match
        return tree.get(new Subscription(p.getMatchValue(), p.getServiceName()));
    }
}
