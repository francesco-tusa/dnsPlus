package experiments;

import java.time.Duration;

/**
 *
 * @author uceeftu
 */
public abstract class ExperimentTask implements Runnable {
    
    private String name;
    private Duration duration;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }


    public long getDuration() {
        return duration.toMillis();
    }
    
    protected void setRequestDuration(Duration d) {
        duration = d;
    }

    @Override
    public abstract void run();
    
}
