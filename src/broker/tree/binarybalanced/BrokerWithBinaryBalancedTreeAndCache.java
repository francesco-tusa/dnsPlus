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

    public BinaryBalancedPublicationTree getCache() {
        return cache;
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
        
        Publication cacheFound = cache.addNode(p);
        System.out.println("Publication " + p.getServiceName() + (cacheFound==null? " added to " : " *** already in *** ") + "the cache");
        
        // null if there was not match
        return matched;
    }
    

    @Override
    public Publication matchSubscription(Subscription s) 
    {   
        // add the subscription to the sub table
        super.addSubscription(s);
        
        //check the cache and return a publication if found
        Publication cacheFound = cache.search(s);
        System.out.println(s.getServiceName() + (cacheFound==null? " not found " : " *** found *** ") + "in the cache");
        return cacheFound;
    }
}
