package broker.balancedbinarytree;

import java.util.Map;
import java.util.TreeMap;
import broker.AbstractBroker;
import publishing.Publication;
import subscribing.Subscription;

/**
 *
 * @author uceeftu
 */
public class BalancedBinaryTree 
{    
    AbstractBroker broker;

    Map<Subscription, Integer> tree;

    public BalancedBinaryTree(AbstractBroker b) 
    {
        broker = b;
        tree = new TreeMap<>(new SubscriptionComparator(broker));
    }    
    
    public void addNode(Subscription s) 
    {
        tree.put(s, 0);
    }
    
    
    public Boolean search(Publication p)
    {
        // a subscription is created from the publication for searching        
        return tree.containsKey(new Subscription(p.getMatchValue()));
    }
}
