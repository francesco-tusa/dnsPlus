package broker;

import subscribing.Subscription;
import publishing.Publication;
import encryption.HEPS;
import java.math.BigInteger;

/**
 *
 * @author uceeftu
 */
public abstract class AbstractBroker {

    protected String name;
    protected HEPS heps;

    public AbstractBroker() {}

    public Integer match(Publication p, Subscription s) {
        return compare(p.getMatchValue(), s.getMatchValue(), s.getMatchValuePlusOne());
    }

    public Integer match(Subscription s1, Subscription s2) {
        return compare(s1.getCoverValue(), s2.getMatchValue(), s2.getMatchValuePlusOne());
    }

    public abstract void addSubscription(Subscription s);

    public abstract boolean matchPublication(Publication p);

    
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
            System.err.println(ex.getMessage());
            return null;
        }
    }
}