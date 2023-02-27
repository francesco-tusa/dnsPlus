package naming;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author f.tusa
 */
public class HEPS 
{
    /**
    * p and q are two large primes. 
    * lambda = lcm(p-1, q-1) = (p-1)*(q-1)/gcd(p-1, q-1).
    */
    private BigInteger p,  q,  lambda, mu;
    
    /**
     * n = p*q, where p and q are two large primes.
     */
    public BigInteger n;
    
    /**
     * nsquare = n*n
     */
    public BigInteger nsquare;
    
    /**
     * a random integer in Z*_{n^2} where gcd (L(g^lambda mod n^2), n) = 1.
     */
    private BigInteger g;
    
    /**
     * number of bits of modulus
     */
    private int bitLength;
    
    // this will be randomly generated for each context (SPEnc)
    private BigInteger r, t;
    
    // the number of bits used to represent the actual information
    private int l; // this is the number of bits used to represent each portion of the address
    
    // used to calculate the range of the random numbers to be generated
    private int s;
    private int u;
    
    
    
    public HEPS(int bitLengthVal, int certainty, int l) {
        keyGeneration(bitLengthVal, certainty, l);
    }
      
    /**
    * Constructs an instance of the Paillier cryptosystem.
    * @param bitLengthVal number of bits of modulus
    * @param certainty The probability that the new BigInteger represents a prime number will exceed (1 - 2^(-certainty)). The execution time of this constructor is proportional to the value of this parameter.
    */
    public HEPS(int bitLengthVal, int certainty) {
        keyGeneration(bitLengthVal, certainty, 128);
    }

    /**
     * Constructs an instance of the Paillier cryptosystem with 512 bits of modulus and at least 1-2^(-64) certainty of primes generation.
     */
    public HEPS() {
        keyGeneration(512, 64, 128);
    }
    

    /**
     * Sets up the public key and private key.
     * @param bitLengthVal number of bits of modulus.
     * @param certainty The probability that the new BigInteger represents a prime number will exceed (1 - 2^(-certainty)). The execution time of this constructor is proportional to the value of this parameter.
     */
    public void keyGeneration(int bitLengthVal, int certainty, int l) {
        this.l = l;
        bitLength = bitLengthVal;
        /*Constructs two randomly generated positive BigIntegers that are probably prime, with the specified bitLength and certainty.*/
        p = new BigInteger(bitLength / 2, certainty, new Random());
        q = new BigInteger(bitLength / 2, certainty, new Random());

        n = p.multiply(q);
        nsquare = n.multiply(n);
        
        g = BigInteger.TWO;
        
        lambda = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE))
                .divide(p.subtract(BigInteger.ONE).gcd(q.subtract(BigInteger.ONE)));
        
        /* check whether g is good.*/
        if (g.modPow(lambda, nsquare).subtract(BigInteger.ONE).divide(n).gcd(n).intValue() != 1) {
            System.out.println("g is not good. Choose g again.");
            System.exit(1);
        }
        
        mu = g.modPow(lambda, nsquare).subtract(BigInteger.ONE).divide(n).modInverse(n);
        
        System.out.println("n: " + n);
        System.out.println("bitlength: " + bitLength);
        
        s = bitLength;
        u = ((s-1) - l)/2;
        
        System.out.println("s: " + s);
        System.out.println("u: " + u);
        System.out.println("l: " + this.l);
        r = new BigInteger(u-this.l, new Random());
        t = new BigInteger(u-this.l, new Random());        
    }
    
    
    /**
     * Encrypts plaintext m. ciphertext c = g^m * r^n mod n^2. This function explicitly requires random input r to help with encryption.
     * @param m plaintext as a BigInteger
     * @param r random plaintext to help with encryption
     * @return ciphertext as a BigInteger
     */
    private BigInteger encryption(BigInteger m, BigInteger rand) {
        return g.modPow(m, nsquare).multiply(rand.modPow(n, nsquare)).mod(nsquare);
    }
    

    private BigInteger shiftedEncryption(BigInteger m) {
        BigInteger rand = (new BigInteger(bitLength, new Random()));
        return encryption(m, rand).modPow(lambda, nsquare);
    }
    
    
    public BigInteger shiftedDecryption(BigInteger c) {
        return (c.mod(nsquare).subtract(BigInteger.ONE)).divide(n).multiply(mu).mod(n);
    }
    
    
    public List<BigInteger> generateSubParameters()
    {
        List<BigInteger> params = new ArrayList<>();
        
        BigInteger param1 = shiftedEncryption(r.multiply(BigInteger.valueOf(-1)));
        BigInteger param2 = shiftedEncryption(BigInteger.valueOf(-1));
        BigInteger param3 = g.modPow(t.multiply(BigInteger.valueOf(-1)), nsquare)
                             .multiply(shiftedEncryption(r.multiply(BigInteger.valueOf(-1))));
        
        params.add(param1);
        params.add(param2);
        params.add(param3);
        
        return params;
    
    }
    
    
    public List<BigInteger> generatePubParameters()
    {
        List<BigInteger> params = new ArrayList<>();
        
        BigInteger param1 = shiftedEncryption(r);
        BigInteger param2 = shiftedEncryption(BigInteger.valueOf(1));
        BigInteger param3 = g.modPow(t, nsquare).multiply(shiftedEncryption(r));
        
        params.add(param1);
        params.add(param2);
        params.add(param3);
        
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
        return s-u-1;
    }
    
}
