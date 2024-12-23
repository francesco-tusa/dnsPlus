package broker;

import subscribing.Subscription;
import publishing.Publication;
import encryption.HEPS;
import broker.subscriptions.balancedbinarytree.SubscriptionBalancedBinaryTree;

/**
 *
 * @author f.tusa
 */
public class BrokerWithBalancedTree extends AbstractBroker {
    
    private SubscriptionBalancedBinaryTree table;

    public BrokerWithBalancedTree(String name, HEPS heps) 
    {
        this.name = name;
        this.heps = heps;
    
        table = new SubscriptionBalancedBinaryTree(this);
    }
    
    
    @Override
    public void addSubscription(Subscription s) 
    {
        if (table.addNode(s) != null)
            System.out.println("Subscription " + s.getServiceName() + " already in the table");
        else
            System.out.println("Subscription " + s.getServiceName() + " added to the table");
    }
    
    @Override
    public boolean matchPublication(Publication p) 
    {   
        Boolean found = table.search(p);
        System.out.println("Found " + p.getServiceName() + " in the sub table: " + found);
        return found;
        
    }

}
