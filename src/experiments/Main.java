package experiments;

import experiments.cache.DNSWithCacheExperiment;
import experiments.cache.asynchronous.DNSWithCacheAsynchronousExperiment;
import experiments.cache.asynchronous.DNSWithCacheAsynchronousSequentialParallelExperiment;
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
        //Experiment experiment = new DNSWithCacheAsynchronousSequentialParallelExperiment("ranked_websites.csv", 1);
        Experiment experiment = new DNSWithCacheAsynchronousSequentialParallelExperiment("ranked_websites.csv", 2, 1, 3);
        
        //Experiment experiment = new DNSWithCacheAsynchronousExperiment("ranked_websites.csv", 1);
        //Experiment experiment = new DNSWithCacheAsynchronousExperiment("ranked_websites.csv", 1, 1000, 100, 1, 2);
        
        //Experiment experiment = new DNSWithCacheExperiment("websites.txt", 1);
        //Experiment experiment = new DNSWithCacheExperiment("websites.txt", 1, 1000, 100);
        
        experiment.start();  
        //experiment2.start();   
    }
    
}
