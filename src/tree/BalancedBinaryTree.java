package tree;

import java.util.Map;
import java.util.TreeMap;
import naming.AbstractBroker;
import naming.Publication;
import naming.Subscription;

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
        // this is a fake subscription used only for searching
        // inside the map
        Subscription s = new Subscription(p.getValue());
        
        return tree.containsKey(s);
    }
}
