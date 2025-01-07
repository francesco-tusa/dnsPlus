package experiments.measurement;

/**
 *
 * @author uceeftu
 */
public class BrokerStats {
    
    private int numberOfPublications;
    private int numberOfSubscriptions;
    private int numberOfMatches;
    private int numberOfCacheHits;

    // not needed
    public BrokerStats() {
        this.numberOfPublications = 0;
        this.numberOfSubscriptions = 0;
        this.numberOfMatches = 0;
        this.numberOfCacheHits = 0;
    }
    
    public int getNumberOfPublications() {
        return numberOfPublications;
    }

    public int getNumberOfSubscriptions() {
        return numberOfSubscriptions;
    }

    public int getNumberOfMatches() {
        return numberOfMatches;
    }

    public int getNumberOfCacheHits() {
        return numberOfCacheHits;
    }
    
    public synchronized void incrementSubscriptions() {
        numberOfSubscriptions++;
    }
    
    public synchronized void incrementPublications() {
        numberOfPublications++;
    }
    
    public synchronized void incrementMatches() {
        numberOfMatches++;
    }
    
    public synchronized void incrementCacheHits() {
        numberOfCacheHits++;
    }

    @Override
    public String toString() {
        return "BrokerStats{" + "numberOfPublications=" + numberOfPublications + ", numberOfSubscriptions=" + numberOfSubscriptions + ", numberOfMatches=" + numberOfMatches + ", numberOfCacheHits=" + numberOfCacheHits + '}';
    }
}
