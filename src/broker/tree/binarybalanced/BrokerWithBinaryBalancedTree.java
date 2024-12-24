package broker.tree.binarybalanced;

import broker.AbstractBroker;
import subscribing.Subscription;
import publishing.Publication;
import encryption.HEPS;

/**
 *
 * @author f.tusa
 */
public class BrokerWithBinaryBalancedTree extends AbstractBroker {
    
    private BinaryBalancedSubscriptionTree table;

    public BrokerWithBinaryBalancedTree(String name, HEPS heps) 
    {
        this.name = name;
        this.heps = heps;
    
        table = new BinaryBalancedSubscriptionTree(this);
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
    public Subscription matchPublication(Publication p) 
    {   
        Subscription found = table.search(p);
        System.out.println(p.getServiceName() + (found==null? " not found" : " found") + " in the sub table");
        return found;
    }
}
