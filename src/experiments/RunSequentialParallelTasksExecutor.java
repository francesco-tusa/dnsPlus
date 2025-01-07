package experiments;

import java.util.logging.Level;
import java.util.logging.Logger;
import utils.CustomLogger;

/**
 *
 * @author uceeftu
 */
public abstract class RunSequentialParallelTasksExecutor extends DefaultRunTasksExecutor {
    
     private static final Logger logger = CustomLogger.getLogger(RunSequentialParallelTasksExecutor.class.getName());

    public RunSequentialParallelTasksExecutor(String name) {
        super(name);
    }

    @Override
    public void start() {
        for (Task task : getTasks()) {
            Thread t = new Thread(task, task.getName());
            t.start();
            try {
                t.join();
            } catch (InterruptedException ex) {
                logger.log(Level.WARNING, "Thread {0} was interrupted",  Thread.currentThread().getName());
            }
        }
    }

}
