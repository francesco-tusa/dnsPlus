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
    
    // for debug only
    private String serviceName;
    
    
    public Subscription(BigInteger mv, BigInteger mvPlusOne, BigInteger cv) {
        matchValue = mv;
        matchValuePlusOne = mvPlusOne;
        coverValue = cv;
    }
    
     public Subscription(BigInteger mv, BigInteger mvPlusOne, BigInteger cv, String serviceName) {
        matchValue = mv;
        matchValuePlusOne = mvPlusOne;
        coverValue = cv;
        this.serviceName = serviceName;
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

    public String getServiceName() {
        return serviceName;
    }
}
