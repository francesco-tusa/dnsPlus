package experiments.cache.asynchronous;

import experiments.Task;
import experiments.Task.TaskType;
import experiments.cache.asynchronous.tasks.MeasurementRequesterPublisherTask;
import experiments.cache.asynchronous.tasks.MeasurementRequesterTask;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.CustomLogger;

/**
 *
 * @author uceeftu
 */
public abstract class AsynchronousRunSequentialParallelTasksExecutor extends AsynchronousRunParallelTasksExecutor {
    
    private static final Logger logger = CustomLogger.getLogger(AsynchronousRunSequentialParallelTasksExecutor.class.getName());
    
    private final List<Task> preTasks;
    private final List<Task> postTasks;
    

    public AsynchronousRunSequentialParallelTasksExecutor(String name) {
        super(name);
        preTasks = new ArrayList<>();
        postTasks = new ArrayList<>();
    }

    
    // pre tasks
    public void addPreTask(Task task) {
        logger.log(Level.FINE, "Adding preTasks");
        task.setTaskType(TaskType.PRE_TASK);
        preTasks.add(task);
    }
    
    public void runPreTasks() {
        logger.log(Level.SEVERE, "Running preTasks");
        runTasks(preTasks);     
    } 
    
    public void requestPreTasksMeasurements() {
        logger.log(Level.FINER, "Requesting preTasks Measurements");
        for (Task task : preTasks) {
            if (task instanceof MeasurementRequesterPublisherTask measurementRequesterTask) {
                measurementRequesterTask.requestMeasurement();
            }
        }
    }
    
    
    // tasks    
    public void requestTasksMeasurements() {
        logger.log(Level.FINER, "Requesting tasks Measurements");
        for (Task task : getTasks()) {
            if (task instanceof MeasurementRequesterTask measurementRequesterTask) {
                measurementRequesterTask.requestMeasurement();
            }
        }
    }
    
    
    // post tasks
    public void addPostTask(Task task) {
        logger.log(Level.FINE, "Adding postTasks");
        task.setTaskType(TaskType.POST_TASK);
        postTasks.add(task);
    }
    
    public void runPostTasks() {
        logger.log(Level.SEVERE, "Running postTasks");
        runTasks(postTasks);     
    } 
    

  
    private void runTasks(List<Task> tasks) {
        setRequestsLatch(new CountDownLatch(tasks.size()));
        setRepliesLatch(new CountDownLatch(tasks.size()));
        
        for (Task task : tasks) {
            Thread t = new Thread(task, task.getName());
            t.start();
        }       
    }
    
    
    // run entry point
    @Override
    public void executeRun() {
        executeTasks(() -> runPreTasks(), () -> requestPreTasksMeasurements());
        executeTasks(() -> super.runTasks(), () -> requestTasksMeasurements());
        executeTasks(() -> runPostTasks(), () -> cleanUp());
    }
    
    // supports executeRun and executes either a measurement request
    // or cleanUp as passed via arguments
    private void executeTasks(Runnable tasks, Runnable measureOrClean) {
        tasks.run();
        waitForRequestsCompletion();
        measureOrClean.run();
        waitForRepliesCompletion();
    }
    
    // overrides superclass implementation as it needs to 
    // cleanUp after post tasks execution
    @Override
    public void cleanUp() {
        logger.log(Level.INFO, "Cleaning Up Run after executing all tasks");
        
        for (Task task : postTasks) {
            if (task instanceof AsynchronousTask asyncTask) {
                asyncTask.cleanUp();
            }
        }
    }
    
    @Override
    public void finalise() {
        // adding all the tasks to a list for later 
        // measurement processing
        List<Task> allTasks = preTasks;
        allTasks.addAll(getTasks());
        allTasks.addAll(postTasks);
        
        setTasks(allTasks);
    }
    
}
