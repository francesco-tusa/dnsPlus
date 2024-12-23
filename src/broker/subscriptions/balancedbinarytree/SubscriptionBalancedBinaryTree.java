package broker.subscriptions.balancedbinarytree;

import java.util.Map;
import java.util.TreeMap;
import broker.AbstractBroker;
import publishing.Publication;
import subscribing.Subscription;

/**
 *
 * @author uceeftu
 */
public class SubscriptionBalancedBinaryTree 
{    
    AbstractBroker broker;

    Map<Subscription, Subscription> tree;

    public SubscriptionBalancedBinaryTree(AbstractBroker b) 
    {
        broker = b;
        tree = new TreeMap<>(new SubscriptionComparator(broker));
    }    
    
    public Subscription addNode(Subscription s) 
    {
        return tree.putIfAbsent(s, s);
    }
    
    
    public Boolean search(Publication p)
    {
        // a subscription is created from the publication for searching        
        return tree.containsKey(new Subscription(p.getMatchValue(), p.getServiceName()));
    }
}
