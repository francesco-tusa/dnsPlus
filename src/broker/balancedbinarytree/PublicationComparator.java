package broker.balancedbinarytree;

import java.util.Comparator;
import broker.AbstractBroker;
import publishing.Publication;

/**
 *
 * @author uceeftu
 */
public class PublicationComparator implements Comparator<Publication> {
    
    private final AbstractBroker broker;

    public PublicationComparator(AbstractBroker b) {
        broker = b;
    }

    @Override
    public int compare(Publication p1, Publication p2) 
    {    
        return broker.match(p1, p2);   
    }
}
