package experiments.inputdata;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.Level;
import utils.CustomLogger;

/**
 *
 * @author uceeftu
 */
public class DomainsFileLoader {

    private static final Logger logger = CustomLogger.getLogger(DomainsFileLoader.class.getName());

    private File serviceNames;
    List<DomainEntry> domains;

    public DomainsFileLoader(String fileName) {
        // assuming input file is always inside user home
        serviceNames = new File(System.getProperty("user.home") + "/" + fileName);
        domains = new ArrayList<>();
    }

    public void loadDomainsInfo() throws FileNotFoundException {
        
        boolean hasNamesOnly = hasNamesOnly();
        
        try (Scanner scanner = new Scanner(serviceNames)) {
            if (hasNamesOnly) {
                while (scanner.hasNextLine()) {
                    String domainName = scanner.nextLine();
                    DomainEntry domain = new DomainEntry(domainName);
                    domains.add(domain);
                }
                logger.log(Level.WARNING, "Loaded domain names information only");
            } else {
                while (scanner.hasNextLine()) {
                    String domainInfo = scanner.nextLine();
                    DomainEntry domain = parseInfo(domainInfo);
                    domains.add(domain);
                }
                logger.log(Level.WARNING, "Loaded domain names and frequencies information");
            }
        }
    }
    
    public boolean hasNamesOnly() throws FileNotFoundException {
        Scanner scanner = new Scanner(serviceNames);

        if (scanner.hasNextLine()) {
            String domainInfo = scanner.nextLine();
            String[] splitDomainInfo = domainInfo.split(",");
            scanner.close();
            return splitDomainInfo.length < 2;
        } else {
            // should throw a different exception, using this one for now
            throw new FileNotFoundException("File might be empty");
        }
    }

    public List<DomainEntry> getDomains() {
        return domains;
    }


    private DomainEntry parseInfo(String domainInfo) {
        String[] splitDomainInfo = domainInfo.split(",");
        return new DomainEntry(splitDomainInfo[0], Long.parseLong(splitDomainInfo[1]));
    }
}
