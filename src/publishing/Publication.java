package publishing;

import java.math.BigInteger;

/**
 *
 * @author f.tusa
 */
public class Publication {
    
    private BigInteger matchValue;
 
    private BigInteger coverValue;
    private BigInteger coverValuePlusOne;
    
     // for debug only
    private String serviceName;
    
    public Publication(BigInteger mv, BigInteger cv, BigInteger cvPlusOne) {
        matchValue = mv;
        coverValue = cv;
        coverValuePlusOne = cvPlusOne;
    }
    
    public Publication(BigInteger mv, BigInteger cv, BigInteger cvPlusOne, String name) {
        this(mv, cv, cvPlusOne);
        serviceName = name; // used for debug
    }

    public BigInteger getMatchValue() {
        return matchValue;
    }

    public BigInteger getCoverValue() {
        return coverValue;
    }
    
    public BigInteger getCoverValuePlusOne() {
        return coverValuePlusOne;
    }

    public String getServiceName() {
        return serviceName;
    }
}
