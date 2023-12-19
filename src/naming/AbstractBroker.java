package naming;

import heps.HEPS;
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

    protected int checkDifference(BigInteger value1, BigInteger value2) throws Exception {
        
        //System.out.println("value1 count: " + value1.bitCount());
        //System.out.println("value2 count: " + value2.bitCount());
        
        System.out.println("value1 length: " + value1.bitLength());
        System.out.println("value2 length: " + value2.bitLength());
        
        
        BigInteger d = heps.shiftedDecryption(value1.multiply(value2));
        int result = d.compareTo(heps.getN().divide(BigInteger.valueOf(2)));
        if (!d.equals(0)) {
            return result;
        } else {
            throw new Exception("Error while comparing the values");
        }
    }
    
    
    private Integer compare(BigInteger value1, BigInteger value2, BigInteger value2PlusOne)
    {
        try {
            //int c1 = checkDifference(value1, value2);
            //int c2 = checkDifference(value1, value2PlusOne);
            if (checkDifference(value1, value2) < 0) {
                if (checkDifference(value1, value2PlusOne) > 0) {
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
    

    public Integer match(Publication p, Subscription s) {
        BigInteger bmn = p.getValue();
        BigInteger bm = s.getMatchValue();
        BigInteger b1m = s.getMatchValuePlusOne();
        return compare(bmn, bm, b1m);
    }


    public Integer cover(Subscription s1, Subscription s2) {
        BigInteger bc = s1.getCoverValue();
        // the value generated for match can be used for the cover too
        BigInteger bm = s2.getMatchValue();
        BigInteger b1m = s2.getMatchValuePlusOne();
        return compare(bc, bm, b1m);
    }
    
    
//    public Boolean equalityMatch(Publication p, Subscription s) {
//        BigInteger bmn = p.getValue();
//        BigInteger bm = s.getMatchValue();
//        BigInteger b1m = s.getMatchValuePlusOne();
//        try {
//            if (checkDifference(bmn, bm) < 0 && checkDifference(bmn, b1m) > 0) {
//                return true;
//            } else {
//                return false;
//            }
//        } catch (Exception ex) {
//            System.err.println(ex.getMessage());
//            return null;
//        }
//    }
    
    
    
    public static void main(String[] args) {
        HEPS heps = new HEPS(2048, 2048 / 8, 512);

        Subscriber subscriber = new Subscriber("Subscriber1");
        Publisher publisher = new Publisher("Publisher1");
        AbstractBroker broker = new BrokerWithBalancedTree("Broker1", heps);
        
        subscriber.setHeps(heps);
        publisher.setHeps(heps);
        
        subscriber.getSecurityParameters();
        publisher.getSecurityParameters();
        
        for (int i = 0; i < 1; i++) {
            java.util.Random rand = new java.util.Random();
            BigInteger bi1 = new BigInteger(1024, rand);
            BigInteger bi2 = new BigInteger(1024, rand);

            Publication p = publisher.generatePublication(bi1);
            Subscription s = subscriber.generateSubscription(bi1);

            Integer match = broker.match(p, s);

            System.out.println("match?" + match);
        }
        
        
        
    }
    
    
}
