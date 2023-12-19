package naming;

import heps.Entity;
import java.math.BigInteger;
import java.util.Random;

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
    
    public BigInteger coverBlind(BigInteger m) 
    {   
        BigInteger rv = (new BigInteger(heps.getRandomRange(), new Random())).mod(heps.getR());
        return params.get(3).multiply(params.get(0).modPow(BigInteger.valueOf(1).subtract(m), heps.getNsquare()))
                            .multiply(params.get(1).modPow(rv.multiply(BigInteger.valueOf(-1)), heps.getNsquare())).mod(heps.getNsquare());
    }
    
    
    public Subscription generateSubscription(String name) 
    {   
        //System.out.println("subscription name lenghth (bits): " + name.getBytes().length * 8);
        BigInteger nameAsBigInteger = new BigInteger(name.toLowerCase().getBytes());
        BigInteger nameAsBigIntegerPlusOne = nameAsBigInteger.add(BigInteger.ONE);
        
//        Subscription s = new Subscription(matchBlind(nameAsBigInteger), 
//                                          matchBlind(nameAsBigIntegerPlusOne),
//                                          coverBlind(nameAsBigInteger));

        Subscription s = new Subscription(matchBlind(nameAsBigInteger), 
                                          matchBlind(nameAsBigIntegerPlusOne),
                                          coverBlind(nameAsBigInteger), 
                                          name); // name is for debugging

        return s;   
    }  
    
    public Subscription generateSubscription(BigInteger nameAsBigInteger) 
    {   
        //System.out.println("subscription name lenghth (bits): " + name.getBytes().length * 8);
        BigInteger nameAsBigIntegerPlusOne = nameAsBigInteger.add(BigInteger.ONE);
        
        Subscription s = new Subscription(matchBlind(nameAsBigInteger), 
                                          matchBlind(nameAsBigIntegerPlusOne),
                                          coverBlind(nameAsBigInteger));
        
        return s;   
    }  
    
}
