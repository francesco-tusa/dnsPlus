package experiments;

import experiments.cache.asynchronous.DNSWithCacheAsynchronousExperiment;
import java.util.logging.Logger;
import utils.CustomLogger;
/**
 *
 * @author uceeftu
 */
public class Main {
    
    private static final Logger logger = CustomLogger.getLogger(Experiment.class.getName());
    
    
    public static void main(String[] args) {
        //DNSWithCacheAsynchronousExperiment experiment = new DNSWithCacheAsynchronousExperiment("websites.txt", 1);
        DNSWithCacheAsynchronousExperiment experiment = new DNSWithCacheAsynchronousExperiment("websites.txt", 1, 1000, 100);
        experiment.executeExperiment();
        experiment.calculateStats();
        System.out.println(experiment);
    }
    
}
