package experiments.inputdata;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import utils.CustomLogger;
import java.util.logging.Level;

/**
 *
 * @author uceeftu
 */
public class DBFactory {
    
    private static final Map<String, DomainsDB> DBMap = new HashMap<>();
  
    private DBFactory () {}
    
    public static DomainsDB getDomainsDB(String fileName) {
        DomainsDB DB;
        try {
            
            if (DBMap.containsKey(fileName)) {
                return DBMap.get(fileName);
            } else {
                DomainsFileLoader loader = new DomainsFileLoader(fileName);
                loader.loadDomainsInfo();
                if (loader.hasNamesOnly()) {
                    DB = new NamesDB(loader.getDomains());
                } else
                    DB = new NamesAndFrequenciesDB(loader.getDomains());
            } 
        } catch (FileNotFoundException ex) {
            CustomLogger.getLogger(DBFactory.class.getName()).log(Level.WARNING, "Input data not found, using a default list of domain names");
            DB = new NamesDB();
        }
        
        return DB;
    }
    
    
    public static void main(String[] args) {
        DomainsDB domainsDB = getDomainsDB("ranked_domains.csv");
        if (domainsDB instanceof NamesAndFrequenciesDB freqDB) {
            
            CustomLogger.getLogger(DBFactory.class.getName()).log(Level.INFO, freqDB.toString());

            List<String> randomEntries = domainsDB.getRandomEntries(500);
            Collections.sort(randomEntries);
            for (String name : randomEntries) {
                System.out.println(name);
            }
        }
    }
}
