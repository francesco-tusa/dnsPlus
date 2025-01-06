package experiments.outputdata;

/**
 *
 * @author uceeftu
 */
public class ExperimentTaskStats {
    private final String name;
    private double averageDuration;
    private double durationStandardDeviation;

    public ExperimentTaskStats(String name) {
        this.name = name;
    }
    

    public double getAverageDuration() {
        return averageDuration;
    }

    public double getDurationStandardDeviation() {
        return durationStandardDeviation;
    }

    public String getName() {
        return name;
    }

    public void setAverageDuration(double averageDuration) {
        this.averageDuration = averageDuration;
    }

    public void setDurationStandardDeviation(double durationStandardDeviation) {
        this.durationStandardDeviation = durationStandardDeviation;
    }

    @Override
    public String toString() {
        return "\n\t\t{name=" + getName() + 
               ", averageDuration=" + getAverageDuration() + 
               ", durationStandardDeviation=" + getDurationStandardDeviation() + '}';
    }
}
