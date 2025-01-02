package experiments;

import java.util.List;

/**
 *
 * @author uceeftu
 * @param <T>
 */
public interface Run<T extends Task> {
    
    String getName();

    void setUp();

    void start();

    void cleanUp();

    void addTask(T task);

    List<T> getTasks();
    
}
