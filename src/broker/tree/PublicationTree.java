package broker.tree;

import publishing.Publication;
import subscribing.Subscription;

/**
 *
 * @author uceeftu
 */
public interface PublicationTree {

    Publication addNode(Publication p);

    Publication search(Subscription s);
    
}
