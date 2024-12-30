package experiments.inputdata;

import java.util.List;

/**
 *
 * @author uceeftu
 */
public interface DomainDB {
    String getRandomEntry();
    List<String> getRandomEntries(int n);
}
