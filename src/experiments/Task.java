package experiments;

import java.time.Duration;

/**
 *
 * @author uceeftu
 */
public interface Task extends Runnable {
    String getName();
    void setName(String name);
    long getDuration();
    void setDuration(Duration duration);
    long getReplyDuration();
    void setReplyDuration(Duration duration);
}
