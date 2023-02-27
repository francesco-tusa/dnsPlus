package naming;

import java.math.BigInteger;
import java.util.List;

/**
 *
 * @author f.tusa
 */
public class Subscriber extends Entity {

    public Subscriber(String name) {
        this.name = name;
    }
    
    @Override
    public void getSecurityParameters() 
    {
        params = heps.generateSubParameters();
    }
    
    
    @Override
    public BigInteger matchBlind(BigInteger m) 
    {        
        return params.get(2).multiply(params.get(0).modPow(m.subtract(BigInteger.valueOf(1)), heps.getNsquare()));
    }
    
    
    public Subscription generateSubscription(String name) 
    {    
        BigInteger nameAsBigInteger = new BigInteger(name.getBytes());        
        Subscription s = new Subscription(matchBlind(nameAsBigInteger), matchBlind(nameAsBigInteger.add(BigInteger.ONE)));
        return s;   
    }  
    
}
