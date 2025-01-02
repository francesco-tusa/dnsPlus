package broker.tree.binarybalanced;

import broker.PublicationComparator;
import broker.tree.PublicationTree;
import java.util.Map;
import java.util.TreeMap;
import broker.AbstractBroker;
import java.util.Collections;
import publishing.Publication;
import subscribing.Subscription;

/**
 *
 * @author uceeftu
 */
public class BinaryBalancedPublicationTree implements PublicationTree 
{    
    AbstractBroker broker;

    Map<Publication, Publication> tree;

    public BinaryBalancedPublicationTree(AbstractBroker b) 
    {
        broker = b;
        tree = Collections.synchronizedMap(new TreeMap<>(new PublicationComparator(broker)));
    }
    
    public BinaryBalancedPublicationTree(AbstractBroker b, PublicationComparator comparator) 
    {
        broker = b;
        tree = Collections.synchronizedMap(new TreeMap<>(comparator));
    }
    
    @Override
    public Publication addNode(Publication p) 
    {
        //System.out.println("Adding to the cache: " + p.getServiceName());
        return tree.putIfAbsent(p, p);
    }
    
    
    @Override
    public Publication search(Subscription s) 
    {
        return tree.get(new Publication(s.getMatchValue(), s.getMatchValuePlusOne(), s.getServiceName()));  
    }
}
