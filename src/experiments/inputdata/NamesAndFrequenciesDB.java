package experiments.inputdata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 *
 * @author uceeftu
 */
public final class NamesAndFrequenciesDB implements DomainsDB {

    private List<DomainEntry> domainEntries;

    public NamesAndFrequenciesDB(List<DomainEntry> domainsList) {
        Collections.sort(domainsList, (d1, d2) -> Long.compare(d1.getFrequency(), d2.getFrequency()));
        this.domainEntries = domainsList;
        calculateCDF();
    }
    
    private void calculateCDF() {
        long totalFrequency = 0;
        for (DomainEntry domain : domainEntries) {
            totalFrequency += domain.getFrequency();
        }
        
        double cumulativeSum = 0;
        for (DomainEntry domain : domainEntries) {
            cumulativeSum += domain.getFrequency();
            domain.setProbability(cumulativeSum / totalFrequency);
        }
    }

    @Override
    public String getRandomEntry() {
        Random random = new Random();
        double randomNumber = random.nextDouble();
        
        for (DomainEntry domain : domainEntries) {
            if (randomNumber < domain.getProbability())
                return domain.getName();
        }
        
        // should never happen
        return null;
    }

    @Override
    public List<String> getRandomEntries(int n) {
        List<String> randomEntries = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            randomEntries.add(getRandomEntry());
        }

        return randomEntries;
    }
    
    @Override
    public String toString() {
        return domainEntries.toString();
    }
    
}
