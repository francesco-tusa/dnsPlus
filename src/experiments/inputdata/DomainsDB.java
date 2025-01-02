package experiments.inputdata;

import java.util.List;

/**
 *
 * @author uceeftu
 */
public interface DomainsDB {
    String getRandomEntry();
    List<String> getRandomEntries(int n);
}
