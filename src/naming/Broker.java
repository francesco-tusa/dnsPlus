package naming;

import tree.BinaryTree;
import tree.Node;

/**
 *
 * @author f.tusa
 */
public class Broker extends AbstractBroker {
    
    
    private BinaryTree table;
    

    public Broker(String n, HEPS heps) 
    {
        this.name = n;
        this.heps = heps;
        table = new BinaryTree(this);
    }
    
    
    public void addSubscription(Subscription s) 
    {
        table.addNode(s);
    }
    
    public boolean matchPublication(Publication p) 
    {
        Node node = table.search(table.getRoot(), p);
        if (node != null)
        {
            //System.out.println("found match: " + node.getSubscription().getServiceName());
            return true;
        }
        
        return false;
        
    }
}
