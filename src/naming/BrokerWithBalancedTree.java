package naming;

import tree.BalancedBinaryTree;

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
    
    
    public void addSubscription(Subscription s) 
    {
        table.addNode(s);
    }
    
    public boolean matchPublication(Publication p) 
    {   
        Boolean found = table.search(p);
        //if (found)
        //    System.out.println("match found");
        return found;
        
    }

}
