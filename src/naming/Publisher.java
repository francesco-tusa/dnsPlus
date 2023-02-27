package naming;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;

/**
 *
 * @author f.tusa
 */
public class Publisher extends Entity {
    
    
     public Publisher(String name) {
        this.name = name;
    }
    
    public void getSecurityParameters() 
    {
        params = heps.generatePubParameters();
    }
    
    @Override
    public BigInteger matchBlind(BigInteger m) {
        BigInteger rv = (new BigInteger(heps.getRandomRange(), new Random())).mod(heps.getR());
        
        return params.get(2).multiply(params.get(0).modPow(m.subtract(BigInteger.valueOf(1)), heps.getNsquare()))
                     .multiply(params.get(1).modPow(rv, heps.getNsquare()));
    }
    
    public Publication generatePublication(String name) { 
        BigInteger nameAsBigInteger = new BigInteger(name.getBytes());        
        Publication p = new Publication(matchBlind(nameAsBigInteger));
        return p;
        
    }

   
    
}
