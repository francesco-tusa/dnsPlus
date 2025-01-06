package broker;

import encryption.BlindedMatchingBroker;
import java.util.Comparator;
import publishing.Publication;

/**
 *
 * @author uceeftu
 */
public class PublicationComparator implements Comparator<Publication> {
    
    private final BlindedMatchingBroker broker;

    public PublicationComparator(BlindedMatchingBroker b) {
        broker = b;
    }

    @Override
    public int compare(Publication p1, Publication p2) 
    {    
        return broker.match(p1, p2);   
    }
}
