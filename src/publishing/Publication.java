package publishing;

import java.math.BigInteger;
import java.util.List;

/**
 *
 * @author f.tusa
 */
public class Publication {
    
    private BigInteger matchValue;
 
    private BigInteger coverValue;
    private BigInteger coverValuePlusOne;
    
    private List<String> recipients; // this is filled in by the broker
    private String publisher;
    
     // for debug only
    private String serviceName;
    
    protected Publication () {
        serviceName = "MEASUREMENT_PILL";
    }
    
    public Publication(String pub) {
        serviceName = "POISON_PILL";
        publisher = pub;
    }
    
    public Publication(BigInteger mv, BigInteger cv, BigInteger cvPlusOne) {
        matchValue = mv;
        coverValue = cv;
        coverValuePlusOne = cvPlusOne;
    }
    
    public Publication(BigInteger mv, BigInteger cv, BigInteger cvPlusOne, String name) {
        this(mv, cv, cvPlusOne);
        serviceName = name; // used for debug
    }
    
    // used for creating publications for matching subscriptions
    // in the publication table
    public Publication(BigInteger cv, BigInteger cvPlusOne, String name) {
        coverValue = cv;
        coverValuePlusOne = cvPlusOne;
        serviceName = name;
    }
    
    public Publication(BigInteger mv, String name) {
        matchValue = mv;
        serviceName = name;
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

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

}
