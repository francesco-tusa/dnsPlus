package broker.tree.binary;

import broker.AbstractBroker;
import subscribing.Subscription;
import publishing.Publication;
import encryption.HEPS;

/**
 *
 * @author f.tusa
 */
public class BrokerWithBinaryTree extends AbstractBroker {
    
    
    private BinarySubscriptionTree table;
    

    public BrokerWithBinaryTree(String n, HEPS heps) 
    {
        this.name = n;
        this.heps = heps;
        table = new BinarySubscriptionTree(this);
    }
    
    
    @Override
    public void addSubscription(Subscription s) 
    {
        table.addNode(s);
    }
    
    @Override
    public Subscription matchPublication(Publication p) 
    {
        Subscription found = table.search(p);
        System.out.println(p.getServiceName() + (found==null? " not found" : " found") + " in the sub table");
        return found;
    }
}
