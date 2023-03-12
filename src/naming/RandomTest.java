package naming;

import java.math.BigInteger;
import java.util.Random;

/**
 *
 * @author uceeftu
 */
public final class RandomTest 
{    
    HEPS heps;
        
    Subscriber subscriber;
    Publisher publisher;
    Broker broker;
    
    int iterations;
    int keySize;
    int l; // the number of bits used to represent the service name
    
    
    public RandomTest(int ks, int ll, int i) 
    {       
        keySize = ks;
        l = ll;
        iterations = i;
        
        heps = new HEPS(keySize, keySize/8, l);
        subscriber = new Subscriber("Subscriber1");
        publisher = new Publisher("Publisher1");
        broker = new Broker("Broker1", heps);
        
        subscriber.setHeps(heps);
        publisher.setHeps(heps);
        
        subscriber.getSecurityParameters();
        publisher.getSecurityParameters();
    }
    
    private String generateString(int length) 
    {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = length;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int)(random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        
        return buffer.toString();
    }
    
    
    private BigInteger generateBigInteger(int bitCount) {
        Random r = new Random();
        return new BigInteger(bitCount, r);
    }
    
    
    public void runTest()
    {
        long t;
        double pub = 0, sub = 0, match = 0, cover = 0;
        long avgBitCount = 0;
        
        for (int i = 0; i < iterations; i++)
        {
            BigInteger m = generateBigInteger(l);
            avgBitCount += m.bitCount();
            
            t = System.nanoTime();
            Publication p = publisher.generatePublication(m);
            pub += System.nanoTime() - t;
            
            m = generateBigInteger(l);
            t = System.nanoTime();
            Subscription s = subscriber.generateSubscription(m);
            sub += System.nanoTime()- t;
            
            t = System.nanoTime();
            broker.match(p, s);
            match += System.nanoTime() - t;
            
            // generating new random subscription for cover
            m = generateBigInteger(l);
            Subscription s2 = subscriber.generateSubscription(m);
            
            t = System.nanoTime();
            broker.cover(s, s2);
            cover += System.nanoTime() - t;    
        }
        
        System.out.println();
        System.out.println("Key size: " + keySize + " l: " + l);
        System.out.println("Average bit count: " + avgBitCount / iterations);
        System.out.println("Publication blind (ms): " + (pub / 1000000) / iterations);
        System.out.println("Subscription blind (ms): " + (sub / 1000000) / iterations);
        System.out.println("Match (ms): " + (match / 1000000) / iterations);
        System.out.println("Cover (ms): " + (cover / 1000000) / iterations);
        System.out.println();
        
    }
    
    
    
    public static void main(String[] args) 
    {
        int iterations = 1000;
        int[] keys = {512, 1024, 1536, 2048, 3072, 4096};
        //int[] keys = {2048};
        //int[] lArray = {64, 96, 128, 160, 192, 224, 256};
        int[] lArray = {128};
        
        RandomTest test;
        
        for (int key : keys)
            for (int l : lArray) 
            {
                if (l >= key/2) break;
                
                test = new RandomTest(key, l, iterations);
                test.runTest();
            }
    }
}
