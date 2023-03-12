package naming;

import java.math.BigInteger;

/**
 *
 * @author uceeftu
 */
public abstract class AbstractBroker {
    
    protected String name;
    protected HEPS heps;

    public AbstractBroker() {
    }

    public abstract void addSubscription(Subscription s);

    public abstract boolean matchPublication(Publication p);

    protected int compare(BigInteger value1, BigInteger value2) throws Exception {
        BigInteger d = heps.shiftedDecryption(value1.multiply(value2));
        int result = d.compareTo(heps.getN().divide(BigInteger.valueOf(2)));
        if (!d.equals(0)) {
            return result;
        } else {
            throw new Exception("Error while comparing the values");
        }
    }

    public Integer match(Publication p, Subscription s) {
        BigInteger vPub = p.getValue();
        BigInteger x1Sub = s.getMatchValue();
        BigInteger x1SubPlusOne = s.getMatchValuePlusOne();
        try {
            int c1 = compare(vPub, x1Sub);
            int c2 = compare(vPub, x1SubPlusOne);
            if (c1 < 0) {
                if (c2 > 0) {
                    return 0; // equality
                } else {
                    return 1;
                }
            } else {
                return -1;
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            return null;
        }
    }

    public Boolean equalityMatch(Publication p, Subscription s) {
        BigInteger vPub = p.getValue();
        BigInteger x1Sub = s.getMatchValue();
        BigInteger x1SubPlusOne = s.getMatchValuePlusOne();
        try {
            if (compare(vPub, x1Sub) < 0 && compare(vPub, x1SubPlusOne) > 0) {
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            return null;
        }
    }

    public Integer cover(Subscription s1, Subscription s2) {
        BigInteger value1 = s1.getCoverValue();
        // the value generated for match can be used for the cover too
        BigInteger value2 = s2.getMatchValue();
        BigInteger value2PlusOne = s2.getMatchValuePlusOne();
        try {
            int c1 = compare(value1, value2);
            int c2 = compare(value1, value2PlusOne);
            if (c1 < 0) {
                if (c2 > 0) {
                    return 0; // equality
                } else {
                    return 1;
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
