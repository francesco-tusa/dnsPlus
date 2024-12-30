package experiments.inputdata;

/**
 *
 * @author uceeftu
 */
public class DomainEntry {
    private final String name;
    private float frequency;
    private float probability;

    public DomainEntry(String name, float frequency) {
        this.name = name;
        this.frequency = frequency;
    }
    
    public DomainEntry(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public float getFrequency() {
        return frequency;
    }

    public float getProbability() {
        return probability;
    }

    public void setProbability(float probability) {
        this.probability = probability;
    }
    
    
    
    
}
