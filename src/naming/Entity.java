package naming;

import java.math.BigInteger;
import java.util.List;

/**
 *
 * @author f.tusa
 */
public abstract class Entity {
    
    protected String name;
    protected HEPS heps;
    protected List<BigInteger> params;

    public Entity() {
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

    public abstract void getSecurityParameters();
    
    public abstract BigInteger matchBlind(BigInteger m);
    
}
