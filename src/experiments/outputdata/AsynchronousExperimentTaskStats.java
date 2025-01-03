package experiments.outputdata;

/**
 *
 * @author uceeftu
 */
public class AsynchronousExperimentTaskStats extends ExperimentTaskStats {

    private double averageReplyDuration;
    private double replyDurationStandardDeviation;
    
    public AsynchronousExperimentTaskStats(String name) {
        super(name);
    }

    public AsynchronousExperimentTaskStats(String name, double averageReplyDuration, double replyDurationStandardDeviation, double averageDuration, double durationStandardDeviation) {
        super(name, averageDuration, durationStandardDeviation);
        this.averageReplyDuration = averageReplyDuration;
        this.replyDurationStandardDeviation = replyDurationStandardDeviation;
    }

    public double getAverageReplyDuration() {
        return averageReplyDuration;
    }

    public void setAverageReplyDuration(double averageReplyDuration) {
        this.averageReplyDuration = averageReplyDuration;
    }

    public double getReplyDurationStandardDeviation() {
        return replyDurationStandardDeviation;
    }

    public void setReplyDurationStandardDeviation(double replyDurationStandardDeviation) {
        this.replyDurationStandardDeviation = replyDurationStandardDeviation;
    }

    @Override
    public String toString() {
        return "\n\t\t{name=" + getName() + 
               ", averageDuration=" + getAverageDuration() + 
               ", durationStandardDeviation=" + getDurationStandardDeviation() +
               ", averageReplyDuration=" + averageReplyDuration + 
               ", replyDurationStandardDeviation=" + replyDurationStandardDeviation + '}';
    }
}
