package experiments;

import experiments.cache.asynchronous.AsynchronousDNSWithCacheExperiment;
/**
 *
 * @author uceeftu
 */
public class Main {
    public static void main(String[] args) {
        AsynchronousDNSWithCacheExperiment experiment = new AsynchronousDNSWithCacheExperiment("websites.txt", 10);
        experiment.executeExperiment();
        experiment.calculateStats();
        System.out.println(experiment);
    }
    
}
