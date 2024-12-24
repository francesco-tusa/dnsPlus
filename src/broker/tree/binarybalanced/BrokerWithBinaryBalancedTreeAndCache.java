package broker.tree.binarybalanced;

import broker.CachingBroker;
import encryption.HEPS;
import publishing.Publication;
import subscribing.Subscription;

public class BrokerWithBinaryBalancedTreeAndCache extends BrokerWithBinaryBalancedTree implements CachingBroker {
    
    private BinaryBalancedPublicationTree cache;
        
    public BrokerWithBinaryBalancedTreeAndCache(String name, HEPS heps) {
        super(name, heps);
        cache = new BinaryBalancedPublicationTree(this);
    }

    
    @Override
    public Subscription matchPublication(Publication p) {

        Subscription matched = super.matchPublication(p);
        
        // we should check whether there is a cached publication
        // for the same name that needs to be updated
        // (should add the node, e.g., if p.TTL > found)
        //if (!cache.search(p)) {
        //    cache.addNode(p); 
        //}
        
        if (cache.addNode(p) != null) {
            System.out.println("Publication " + p.getServiceName() + " already in the cache");
        } else
            System.out.println("Publication " + p.getServiceName() + " added to the cache");
        
        return matched;
    }
    

    @Override
    public Publication matchSubscription(Subscription s) 
    {   
        // add the subscription to the sub table
        super.addSubscription(s);
        
        //check the cache and return a publication if found
        Publication cacheFound = cache.search(s);
        if (cacheFound == null) {
            System.out.println("No entry for " + s.getServiceName() + " found in the cache");
        } else {
            System.out.println("Entry for " + s.getServiceName() + " found in the cache: " + cacheFound.getServiceName());
        }
            
        return cacheFound;
    }
}
