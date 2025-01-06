package encryption;

import java.math.BigInteger;
import java.util.Random;
import publishing.Publication;
import utils.NameEncoder;

/**
 *
 * @author f.tusa
 */
public abstract class BlindingPublisher extends BlindingEntity {
    
    
     public BlindingPublisher(String name) {
        this.name = name;
    }
    
    @Override
    public void getSecurityParameters() 
    {
        params = heps.generatePubParameters();
    }
    
    @Override
    public BigInteger matchBlind(BigInteger m) {
        BigInteger rv = (new BigInteger(heps.getRandomRange(), new Random())).mod(heps.getR());
        
        return params.get(2).multiply(params.get(0).modPow(m.subtract(BigInteger.valueOf(1)), heps.getNsquare()))
                     .multiply(params.get(1).modPow(rv, heps.getNsquare())).mod(heps.getNsquare());
    }

    @Override
    public BigInteger coverBlind(BigInteger m) {
        return params.get(3).multiply(params.get(0).modPow(BigInteger.ONE.subtract(m), 
                            heps.getNsquare())).mod(heps.getNsquare());
    }
    
    public Publication generatePublication(String name) { 
        BigInteger nameAsBigInteger = NameEncoder.stringToBigInteger(name);        
        return generatePublication(nameAsBigInteger, name);
    }

    public Publication generatePublication(BigInteger nameAsBigInteger, String name) {      
        BigInteger nameAsBigIntegerPlusOne = nameAsBigInteger.add(BigInteger.ONE);
        
        return new Publication(matchBlind(nameAsBigInteger), 
                               coverBlind(nameAsBigInteger),
                               coverBlind(nameAsBigIntegerPlusOne),
                               name); // for debugging
    }
}
