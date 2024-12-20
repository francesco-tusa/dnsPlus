package broker;

import subscribing.Subscription;
import publishing.Publication;
import encryption.HEPS;
import broker.balancedbinarytree.BalancedBinaryTree;

/**
 *
 * @author f.tusa
 */
public class BrokerWithBalancedTree extends AbstractBroker {
    
    private BalancedBinaryTree table;

    public BrokerWithBalancedTree(String n, HEPS heps) 
    {
        this.name = n;
        this.heps = heps;
    
        table = new BalancedBinaryTree(this);
    }
    
    
    @Override
    public void addSubscription(Subscription s) 
    {
        table.addNode(s);
    }
    
    @Override
    public boolean matchPublication(Publication p) 
    {   
        Boolean found = table.search(p);
        return found;
        
    }

}
