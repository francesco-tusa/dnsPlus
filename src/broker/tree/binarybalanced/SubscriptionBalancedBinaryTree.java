package broker.tree.binarybalanced;

import java.util.Map;
import java.util.TreeMap;
import broker.AbstractBroker;
import broker.tree.SubscriptionTree;
import publishing.Publication;
import subscribing.Subscription;

/**
 *
 * @author uceeftu
 */
public class SubscriptionBalancedBinaryTree implements SubscriptionTree 
{    
    AbstractBroker broker;

    Map<Subscription, Subscription> tree;

    public SubscriptionBalancedBinaryTree(AbstractBroker b) 
    {
        broker = b;
        tree = new TreeMap<>(new SubscriptionComparator(broker));
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
        return tree.get(new Subscription(p.getMatchValue(), p.getServiceName()));
    }
}
