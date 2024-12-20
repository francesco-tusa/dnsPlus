package subscribing;

import encryption.Entity;
import java.math.BigInteger;
import java.util.Random;
import utils.NameEncoding;

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
        return params.get(2).multiply(params.get(0).modPow(m.subtract(BigInteger.valueOf(1)), heps.getNsquare())).mod(heps.getNsquare());
    }
    
    @Override
    public BigInteger coverBlind(BigInteger m) 
    {   
        BigInteger rv = (new BigInteger(heps.getRandomRange(), new Random())).mod(heps.getR());
        return params.get(3).multiply(params.get(0).modPow(BigInteger.valueOf(1).subtract(m), heps.getNsquare()))
                            .multiply(params.get(1).modPow(rv.multiply(BigInteger.valueOf(-1)), heps.getNsquare())).mod(heps.getNsquare());
    }
    
    
    public Subscription generateSubscription(String name) 
    {   
        BigInteger nameAsBigInteger = NameEncoding.stringToBigInteger(name);
        BigInteger nameAsBigIntegerPlusOne = nameAsBigInteger.add(BigInteger.ONE);
        
        return new Subscription(matchBlind(nameAsBigInteger), 
                                          matchBlind(nameAsBigIntegerPlusOne),
                                          coverBlind(nameAsBigInteger), 
                                          name); // name is for debugging  
    }  
    
    public Subscription generateSubscription(BigInteger nameAsBigInteger) 
    {  
        BigInteger nameAsBigIntegerPlusOne = nameAsBigInteger.add(BigInteger.ONE);
        
        return new Subscription(matchBlind(nameAsBigInteger), 
                                          matchBlind(nameAsBigIntegerPlusOne),
                                          coverBlind(nameAsBigInteger));  
    }  
}