package experiments.inputdata;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import utils.CustomLogger;
import java.util.logging.Level;

/**
 *
 * @author uceeftu
 */
public class DBFactory {
    
    private static final Map<String, DomainDB> DBMap = new HashMap<>();
  
    private DBFactory () {}
    
    public static DomainDB getDomainsDB(String fileName) {
        DomainDB DB;
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
}