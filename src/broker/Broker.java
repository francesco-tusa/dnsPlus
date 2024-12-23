package broker;

import subscribing.Subscription;
import publishing.Publication;
import encryption.HEPS;
import broker.binarytree.SubscriptionBinaryTree;
import broker.binarytree.SubscriptionBinaryTreeNode;

/**
 *
 * @author f.tusa
 */
public class Broker extends AbstractBroker {
    
    
    private SubscriptionBinaryTree table;
    

    public Broker(String n, HEPS heps) 
    {
        this.name = n;
        this.heps = heps;
        table = new SubscriptionBinaryTree(this);
    }
    
    
    public void addSubscription(Subscription s) 
    {
        table.addNode(s);
    }
    
    public boolean matchPublication(Publication p) 
    {
        SubscriptionBinaryTreeNode node = table.search(table.getRoot(), p);
        if (node != null)
        {
            //System.out.println("found match: " + node.getSubscription().getServiceName());
            return true;
        }
        
        return false;
        
    }
}
