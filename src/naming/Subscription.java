package naming;

import java.math.BigInteger;

/**
 *
 * @author f.tusa
 */
public class Subscription {
    
    BigInteger value;
    BigInteger valuePlusOne;
    
    public Subscription(BigInteger v, BigInteger vPlusOne) {
        value = v;
        valuePlusOne = vPlusOne;     
    }

    public BigInteger getValue() {
        return value;
    }

    public BigInteger getValuePlusOne() {
        return valuePlusOne;
    }
    
    
    
}
