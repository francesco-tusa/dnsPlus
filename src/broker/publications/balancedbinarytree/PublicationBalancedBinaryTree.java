package broker.publications.balancedbinarytree;

import java.util.Map;
import java.util.TreeMap;
import broker.AbstractBroker;
import publishing.Publication;
import subscribing.Subscription;

/**
 *
 * @author uceeftu
 */
public class PublicationBalancedBinaryTree 
{    
    AbstractBroker broker;

    Map<Publication, Publication> tree;

    public PublicationBalancedBinaryTree(AbstractBroker b) 
    {
        broker = b;
        tree = new TreeMap<>(new PublicationComparator(broker));
    }    
    
    public Publication addNode(Publication p) 
    {
        //System.out.println("Adding to the cache: " + p.getServiceName());
        return tree.putIfAbsent(p, p);
    }
    
    
    public Boolean search(Publication p)
    {        
        boolean found = tree.containsKey(p);
        System.out.println("Found " + p.getServiceName() + " in the cache: " + found);
        return found;
    }
    
    
    public Boolean search(Subscription s)
    {
        // a publication is created from the subscription for searching in the cache        
        return tree.containsKey(new Publication(s.getMatchValue(), s.getMatchValuePlusOne(), s.getServiceName()));
    }
    
    
    public Publication searchAndGetPublication(Subscription s) 
    {
        //return tree.get(new Publication(s.getMatchValue(), s.getServiceName()));
        return tree.get(new Publication(s.getMatchValue(), s.getMatchValuePlusOne(), s.getServiceName()));
        
    }
}
