package subscribing;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author f.tusa
 */
public class Subscription {
    
    private BigInteger matchValue;
    private BigInteger matchValuePlusOne;
    
    private BigInteger coverValue;
    
    private List<String> subscribers;
    
    // for debug only
    private String serviceName;
    
    public Subscription(String subscriber) {
        serviceName = "POISON_PILL";
        subscribers = new ArrayList<>();
        subscribers.add(subscriber);
    }
    
    
    public Subscription(BigInteger mv, BigInteger mvPlusOne, BigInteger cv) {
        matchValue = mv;
        matchValuePlusOne = mvPlusOne;
        coverValue = cv;
        subscribers = new ArrayList<>();
    }
    
    public Subscription(BigInteger mv, BigInteger mvPlusOne, BigInteger cv, String name) {
        matchValue = mv;
        matchValuePlusOne = mvPlusOne;
        coverValue = cv;
        subscribers = new ArrayList<>();
        serviceName = name; // used for debug
    }
    
    // this should only be used to create a fake subscription for publication 
    // match in the BalancedBinaryTree
    public Subscription(BigInteger cv, String name) {
        coverValue = cv;
        serviceName = name;
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
    
    public void addSubscriber(String subscriberName) {
        subscribers.add(subscriberName);
    }

    public List<String> getSubscribers() {
        return subscribers;
    }
    
}
