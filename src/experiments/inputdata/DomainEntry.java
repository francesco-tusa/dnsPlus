package experiments.inputdata;

/**
 *
 * @author uceeftu
 */
public class DomainEntry {
    private final String name;
    private long frequency;
    private double probability;

    public DomainEntry(String name, long frequency) {
        this.name = name;
        this.frequency = frequency;
    }
    
    public DomainEntry(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public long getFrequency() {
        return frequency;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    @Override
    public String toString() {
        return """
               
               \tDomainEntry{name=""" + name + ", frequency=" + frequency + ", probability=" + probability + '}';
    }
    
}
