package broker.tree.binarybalanced;

import broker.AbstractBroker;
import subscribing.Subscription;
import publishing.Publication;
import encryption.HEPS;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.CustomLogger;

/**
 *
 * @author f.tusa
 */
public class BrokerWithBinaryBalancedTree extends AbstractBroker {
    
    private static final Logger logger = CustomLogger.getLogger(BrokerWithBinaryBalancedTree.class.getName());
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
            logger.log(Level.FINE, "Subscription for {0} already in the table", s.getServiceName());
        else
            logger.log(Level.FINER, "Subscription {0} added to the table", s.getServiceName());
    }
    
    @Override
    public Subscription matchPublication(Publication p) 
    {   
        Subscription found = table.search(p);
        
        if (found == null) {
            logger.log(Level.FINER, "{0} not found in the subscription table", p.getServiceName());
        } else {
            logger.log(Level.FINE, "{0} found in the subscription table", p.getServiceName());
        }
        return found;
    }
}