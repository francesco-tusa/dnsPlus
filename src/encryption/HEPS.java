package encryption;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import utils.CustomLogger;

/**
 *
 * @author f.tusa
 */
public final class HEPS {
    private BigInteger p, q, lambda, mu;
    private BigInteger n;
    private BigInteger nsquare;
    private BigInteger g;
    private int bitLength;
    private BigInteger r, t;
    private int l;
    private int s;
    private int u;

    // Static instance of the class
    private static HEPS instance;

    // Private constructor to prevent instantiation
    private HEPS(int bitLengthVal, int certainty, int l) {
        keyGeneration(bitLengthVal, certainty, l);
    }

    public static synchronized HEPS getInstance() {
        if (instance == null) {
            // TODO: these should be read from a conf file
            instance = new HEPS(2048, 2048/8, 512);
        }
        return instance;
    }

    public void keyGeneration(int bitLengthVal, int certainty, int l) {
        this.l = l;
        bitLength = bitLengthVal;
        
        p = new BigInteger(bitLength / 2, certainty, new Random());
        q = new BigInteger(bitLength / 2, certainty, new Random());
        
        n = p.multiply(q);
        nsquare = n.multiply(n);
        
        g = BigInteger.valueOf(2);
        
        lambda = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE))
                .divide(p.subtract(BigInteger.ONE).gcd(q.subtract(BigInteger.ONE)));
        
        if (g.modPow(lambda, nsquare).subtract(BigInteger.ONE).divide(n).gcd(n).intValue() != 1) {
            CustomLogger.getLogger(HEPS.class.getName()).log(Level.SEVERE, "g is not good. Choose g again.");
            System.exit(1);
        }
        
        mu = g.modPow(lambda, nsquare).subtract(BigInteger.ONE).divide(n).modInverse(n);
        s = bitLength;
        u = ((s - 1) - l) / 2;
        r = new BigInteger(u - this.l, new Random());
        t = new BigInteger(u - this.l, new Random());
    }

    private BigInteger encryption(BigInteger m, BigInteger rand) {
        return g.modPow(m, nsquare).multiply(rand.modPow(n, nsquare));
    }

    private BigInteger shiftedEncryption(BigInteger m) {
        BigInteger rand = new BigInteger(bitLength, new Random());
        return encryption(m, rand).modPow(lambda, nsquare);
    }

    public BigInteger shiftedDecryption(BigInteger c) {
        return (c.mod(nsquare).subtract(BigInteger.ONE)).divide(n).multiply(mu).mod(n);
    }

    public List<BigInteger> generateSubParameters() {
        List<BigInteger> params = new ArrayList<>();
        BigInteger param1 = shiftedEncryption(r.multiply(BigInteger.valueOf(-1)));
        BigInteger param2 = shiftedEncryption(BigInteger.valueOf(-1));
        BigInteger param3 = g.modPow(t.multiply(BigInteger.valueOf(-1)), nsquare)
                .multiply(shiftedEncryption(r.multiply(BigInteger.valueOf(-1))));
        BigInteger param4 = g.modPow(t, nsquare).multiply(shiftedEncryption(r));
        params.add(param1);
        params.add(param2);
        params.add(param3);
        params.add(param4);
        
        return params;
    }

    public List<BigInteger> generatePubParameters() {
        List<BigInteger> params = new ArrayList<>();
        BigInteger param1 = shiftedEncryption(r);
        BigInteger param2 = shiftedEncryption(BigInteger.valueOf(1));
        BigInteger param3 = g.modPow(t, nsquare).multiply(shiftedEncryption(r));
        BigInteger param4 = g.modPow(t.multiply(BigInteger.valueOf(-1)), nsquare)
                .multiply(shiftedEncryption(r.multiply(BigInteger.valueOf(-1))));
        params.add(param1);
        params.add(param2);
        params.add(param3);
        params.add(param4);
        return params;
    }

    public BigInteger getN() {
        return n;
    }

    public BigInteger getNsquare() {
        return nsquare;
    }

    public BigInteger getR() {
        return r;
    }

    public int getRandomRange() {
        return s - u - 1;
    }
}