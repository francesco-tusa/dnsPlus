package encryption;

import java.math.BigInteger;
import java.util.Random;
import subscribing.Subscription;
import utils.NameEncoder;

/**
 *
 * @author f.tusa
 */
public abstract class BlindingSubscriber extends BlindingEntity {

    public BlindingSubscriber(String name) {
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
        BigInteger nameAsBigInteger = NameEncoder.stringToBigInteger(name);
        return generateSubscription(nameAsBigInteger, name);
    }  
    
    public Subscription generateSubscription(BigInteger nameAsBigInteger, String name) {
        BigInteger nameAsBigIntegerPlusOne = nameAsBigInteger.add(BigInteger.ONE);

        return new Subscription(matchBlind(nameAsBigInteger),
                matchBlind(nameAsBigIntegerPlusOne),
                coverBlind(nameAsBigInteger),
                name); // name is for debugging   
    }  
}