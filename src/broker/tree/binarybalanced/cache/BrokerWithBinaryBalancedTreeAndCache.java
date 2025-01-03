package broker.tree.binarybalanced.cache;

import broker.tree.binarybalanced.BrokerWithBinaryBalancedTree;
import broker.CachingBroker;
import broker.CachingBroker;
import broker.tree.binarybalanced.BinaryBalancedPublicationTree;
import encryption.HEPS;
import java.util.logging.Logger;
import java.util.logging.Level;
import publishing.Publication;
import subscribing.Subscription;
import utils.CustomLogger;

public class BrokerWithBinaryBalancedTreeAndCache extends BrokerWithBinaryBalancedTree implements CachingBroker {
    
    private static final Logger logger = CustomLogger.getLogger(BrokerWithBinaryBalancedTreeAndCache.class.getName());
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
        // this can be implemented by using a different PublicationComparator
        // passed to BinaryBalancedPublicationTree(this, <custom comparator>);
        // in the constructor of this class
        
        Publication cacheFound = cache.addNode(p);
        if (cacheFound == null) {
            logger.log(Level.FINER, "Publication {0} was added to the cache", p.getServiceName());
        } else {
            logger.log(Level.FINE, "Publication {0} was found in the cache", p.getServiceName());
        }
        
        // null if there was not match
        return matched;
    }
    

    @Override
    public Publication cacheLookUp(Subscription s) 
    {   
        // add the subscription to the sub table
        super.addSubscription(s);
        
        //check the cache and return a publication if found
        Publication cacheFound = cache.search(s);
        if (cacheFound == null) {
            logger.log(Level.FINER, "A publication for {0} was not found in the cache", s.getServiceName());
        } else {
            logger.log(Level.FINE, "A publication for {0} was found in the cache", s.getServiceName());
        }
        
        return cacheFound;
    }
}
