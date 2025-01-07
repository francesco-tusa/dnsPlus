package experiments;

import experiments.cache.DNSWithCacheExperiment;
import experiments.cache.asynchronous.DNSWithCacheAsynchronousExperiment;
import java.util.logging.Logger;
import java.util.logging.Level;
import utils.CustomLogger;
/**
 *
 * @author uceeftu
 */
public class Main {
    
    private static final Logger logger = CustomLogger.getLogger(Experiment.class.getName(), Level.INFO);
    
    
    public static void main(String[] args) {
        //Experiment experiment = new DNSWithCacheAsynchronousExperiment("ranked_websites.csv", 1);
        Experiment experiment = new DNSWithCacheAsynchronousExperiment("ranked_websites.csv", 1, 100, 1000, 1, 1);
        //Experiment experiment = new DNSWithCacheExperiment("websites.txt", 1);
        //Experiment experiment = new DNSWithCacheExperiment("websites.txt", 1, 1000, 100);
        
        experiment.start();  
        //experiment2.start();   
    }
    
}
