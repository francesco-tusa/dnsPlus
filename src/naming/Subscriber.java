package naming;

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
        return params.get(2).multiply(params.get(0).modPow(m.subtract(BigInteger.valueOf(1)), heps.getNsquare()));
    }
    
    public BigInteger coverBlind(BigInteger m) 
    {   
        BigInteger rv = (new BigInteger(heps.getRandomRange(), new Random())).mod(heps.getR());
        BigInteger coverBlind = params.get(3).multiply(params.get(0).modPow(BigInteger.valueOf(1).subtract(m), heps.getNsquare()))
                .multiply(params.get(1).modPow(rv.multiply(BigInteger.valueOf(-1)), heps.getNsquare()));
        
        
        //List<BigInteger> params2 = heps.generatePubParameters();
        //BigInteger matchBlind = params2.get(2).multiply(params2.get(0).modPow(m.subtract(BigInteger.valueOf(1)), heps.getNsquare()))
        //        .multiply(params2.get(1).modPow(rv, heps.getNsquare()));
        
        //System.out.println(coverBlind);
        //System.out.println(matchBlind);
        
        
        return coverBlind;
    }
    
    
    public Subscription generateSubscription(String name) 
    {    
        BigInteger nameAsBigInteger = new BigInteger(name.getBytes());
        BigInteger nameAsBigIntegerPlusOne = nameAsBigInteger.add(BigInteger.ONE);
        
        Subscription s = new Subscription(matchBlind(nameAsBigInteger), 
                                          matchBlind(nameAsBigIntegerPlusOne),
                                          coverBlind(nameAsBigInteger),
                                          coverBlind(nameAsBigIntegerPlusOne));
        return s;   
    }  
    
}
