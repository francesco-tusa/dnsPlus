package naming;

import java.math.BigInteger;

/**
 *
 * @author f.tusa
 */
public class Subscription {
    
    private BigInteger matchValue;
    private BigInteger matchValuePlusOne;
    
    private BigInteger coverValue;
    
    
    public Subscription(BigInteger mv, BigInteger mvPlusOne, BigInteger cv) {
        matchValue = mv;
        matchValuePlusOne = mvPlusOne;
        coverValue = cv;
    }

    public BigInteger getMatchValue() {
        return matchValue;
    }

    public BigInteger getMatchValuePlusOne() {
        return matchValuePlusOne;
    }

    public BigInteger getCoverValue() {
        return coverValue;
    }
}
