package experiments;

import experiments.cache.asynchronous.DNSWithCacheAsynchronousExperiment;
/**
 *
 * @author uceeftu
 */
public class Main {
    public static void main(String[] args) {
        DNSWithCacheAsynchronousExperiment experiment = new DNSWithCacheAsynchronousExperiment("websites.txt", 2);
        experiment.executeExperiment();
        experiment.calculateStats();
        System.out.println(experiment);
    }
    
}
