package encryption;

import broker.Broker;
import subscribing.Subscription;
import publishing.Publication;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.math.BigInteger;
import utils.CustomLogger;

/**
 *
 * @author uceeftu
 */
public abstract class BlindedMatchingBroker implements Broker {

    private static final Logger logger = CustomLogger.getLogger(BlindedMatchingBroker.class.getName());
    protected String name;
    protected HEPS heps;

    public BlindedMatchingBroker() {}

    public Integer match(Subscription s1, Subscription s2) {
        logger.log(Level.FINE, "Matching Subscriptions: {0} with {1}", new Object[]{s1.getServiceName(), s2.getServiceName()});
        return compare(s1.getCoverValue(), s2.getMatchValue(), s2.getMatchValuePlusOne());
    }
    
    // like match(p, s) but uses the values calculated for cover within the publication
    public Integer match(Publication p1, Publication p2) {
        // swapped argument because of balanced tree, required changing sign: -
        logger.log(Level.FINE, "Matching Publications: {0} with {1}", new Object[]{p1.getServiceName(), p2.getServiceName()});
        return -compare(p2.getMatchValue(), p1.getCoverValue(), p1.getCoverValuePlusOne());
    }
    
    // this is only used by the binary tree implementation (not balanced)
    public Integer match(Publication p, Subscription s) {
        return compare(p.getMatchValue(), s.getMatchValue(), s.getMatchValuePlusOne());
    }

    
    private int checkDifference(BigInteger value1, BigInteger value2) throws Exception {
        BigInteger d = heps.shiftedDecryption(value1.multiply(value2));
        int result = d.compareTo(heps.getN().divide(BigInteger.valueOf(2)));
        if (!d.equals(0)) {
            return result;
        } else {
            throw new Exception("Error while comparing the values");
        }
    }

    private Integer compare(BigInteger value1, BigInteger value2, BigInteger value2PlusOne) {
        try {
            if (checkDifference(value1, value2) < 0) {
                if (checkDifference(value1, value2PlusOne) > 0) {
                    return 0; // equality
                } else {
                    return 1; // value2 is less than value1
                }
            } else {
                return -1;
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage());
            return null;
        }
    }
    
}