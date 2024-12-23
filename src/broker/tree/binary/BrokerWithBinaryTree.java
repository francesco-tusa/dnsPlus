package broker.tree.binary;

import broker.AbstractBroker;
import subscribing.Subscription;
import publishing.Publication;
import encryption.HEPS;
import broker.tree.binary.SubscriptionBinaryTree;
import broker.tree.binary.SubscriptionBinaryTreeNode;

/**
 *
 * @author f.tusa
 */
public class BrokerWithBinaryTree extends AbstractBroker {
    
    
    private SubscriptionBinaryTree table;
    

    public BrokerWithBinaryTree(String n, HEPS heps) 
    {
        this.name = n;
        this.heps = heps;
        table = new SubscriptionBinaryTree(this);
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
