package naming;

import java.math.BigInteger;

/**
 *
 * @author f.tusa
 */
public class Broker {
    
    private HEPS heps;

    public Broker(HEPS heps) {
        this.heps = heps;
    }
    
    
    private Boolean match(BigInteger vPub, BigInteger x1Sub) {
        BigInteger d = heps.shiftedDecryption(vPub.multiply(x1Sub));
        
        int result = d.compareTo(heps.getN().divide(BigInteger.valueOf(2)));
        
        if (!d.equals(0) && result < 0) {
            System.out.println("Publication Value (v) >= Subscription Value (x)");
            return true;
        }
        else {
            System.out.println("Publication Value (v) < Subscription Value (x)");
            return false;
        }
        
        //System.out.println("Raw Difference: " + d);
        //System.out.println("Difference: " + d.subtract(rv).divide(r));
        //return d;
    }
    
    
    //public Boolean brokerEqualityMatch(BigInteger vPub, BigInteger x1Sub, BigInteger x1SubPlusOne) {
    public Boolean equalityMatch(Publication p, Subscription s) {
     
        BigInteger vPub = p.getValue();
        
        BigInteger x1Sub = s.getValue();
        BigInteger x1SubPlusOne = s.getValuePlusOne();
        
        BigInteger d1 = heps.shiftedDecryption(vPub.multiply(x1Sub));
        int result1 = d1.compareTo(heps.getN().divide(BigInteger.valueOf(2)));
        
        BigInteger d2 = heps.shiftedDecryption(vPub.multiply(x1SubPlusOne));
        int result2 = d2.compareTo(heps.getN().divide(BigInteger.valueOf(2)));
        
        if ( (!d1.equals(0) && result1 < 0) && (!d2.equals(0) && result2 > 0))
        {
            System.out.println("Publication Value (v) equals to Subscription Value (x)");
            return true;
        }
        else {
            System.out.println("Publication Value is different from Subscription Value (x)");
            return false;
        }
    }   
}
