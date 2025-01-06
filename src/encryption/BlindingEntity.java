package encryption;

import java.math.BigInteger;
import java.util.List;

/**
 *
 * @author f.tusa
 */
public abstract class BlindingEntity {
    
    protected String name;
    protected HEPS heps;
    protected List<BigInteger> params;

    public BlindingEntity() {
    }

    public String getName() {
        return name;
    }

    public HEPS getHeps() {
        return heps;
    }

    public void setHeps(HEPS heps) {
        this.heps = heps;
    }
    
    public void init() {
        setHeps(HEPS.getInstance());
        getSecurityParameters();
    } 

    public abstract void getSecurityParameters();
    
    public abstract BigInteger matchBlind(BigInteger m);
    
    public abstract BigInteger coverBlind(BigInteger m);
    
}
