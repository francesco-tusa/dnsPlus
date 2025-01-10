package experiments.cache.asynchronous;

import encryption.BlindingEntity;
import experiments.RunTasksOutputManager;
import experiments.Task;
import experiments.cache.asynchronous.tasks.MeasurementRequesterPublisherTask;
import experiments.cache.asynchronous.tasks.MeasurementRequesterSubscriberTask;
import experiments.cache.asynchronous.tasks.PublisherNopTask;
import experiments.cache.asynchronous.tasks.PublisherTask;
import experiments.cache.asynchronous.tasks.SubscriberNopTask;
import experiments.cache.asynchronous.tasks.SubscriberTask;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import publishing.Publisher;
import subscribing.ReceivingSubscriber;

/**
 *
 * @author uceeftu
 */
public final class DNSWithCacheAsynchronousSequentialParallelExperiment extends AsynchronousExperiment {

    DNSWithCacheAsynchronousSequentialParallelRun experimentRun;
    
    private int nPubs;
    private int nSubs;

    public DNSWithCacheAsynchronousSequentialParallelExperiment(String inputFileName, int numberOfRuns) {
        this(inputFileName, numberOfRuns, 1, 1);
    }
    
    
    public DNSWithCacheAsynchronousSequentialParallelExperiment(String inputFileName, 
                                                                int numberOfRuns, 
                                                                int numberOfPubs,
                                                                int numberOfSubs) 
    {
        super(DNSWithCacheAsynchronousSequentialParallelExperiment.class.getSimpleName(), inputFileName, numberOfRuns);
        nPubs = numberOfPubs;
        nSubs = numberOfSubs;
    }
    

    @Override
    protected void executeRun() {
        TaskGenerator taskGenerator = new TaskGenerator();
        experimentRun = new DNSWithCacheAsynchronousSequentialParallelRun();
        experimentRun.setUp();
        
        Publisher pub1 = new Publisher("pub1");
        Publisher pub2 = new Publisher("pub2");
        ReceivingSubscriber sub1 = new ReceivingSubscriber("sub1");
        ReceivingSubscriber sub2 = new ReceivingSubscriber("sub2");
        
        
        taskGenerator.addRandomisedPreTask(pub1, 500);
        
        taskGenerator.addRandomisedTask(sub1, 10);
        //taskGenerator.addRandomisedTask(sub2, 10);
        taskGenerator.addTask(new ReceivingSubscriber("subbb"), new ArrayList<>(Arrays.asList("www.doesnotexist.com")));
        taskGenerator.addRandomisedTask(pub1, 100);
        taskGenerator.addRandomisedTask(pub2, 200);
        
        taskGenerator.addRandomisedPostTask(sub1, 20);
        
        taskGenerator.addRunCleaningTasks();
        
        
        experimentRun.executeRun();
        
        experimentRun.finalise();
        
        List<RunTasksOutputManager> allRunsTasksOutput = getAllRunsTasksOutput();
        allRunsTasksOutput.add(experimentRun);
    }
    
    
    private class TaskGenerator {
        public void addRandomisedPreTask(BlindingEntity entity, int numberOfOperations) {
            addRandomisedTaskHelper(entity, numberOfOperations, (task) -> { experimentRun.addPreTask(task); });
        }
        
        public void addRandomisedTask(BlindingEntity entity, int numberOfOperations) {
            addRandomisedTaskHelper(entity, numberOfOperations, (task) -> { experimentRun.addTask(task); });
        }
        
        public void addRandomisedPostTask(BlindingEntity entity, int numberOfOperations) { 
            addRandomisedTaskHelper(entity, numberOfOperations, (task) -> { experimentRun.addPostTask(task); });
        }
 
        private void addRandomisedTaskHelper(BlindingEntity entity, int numberOfOperations, Consumer<AsynchronousTask> method) {
            AsynchronousTask task;
            switch (entity) {
                case Publisher p -> {
                    task = new MeasurementRequesterPublisherTask(p, experimentRun, getInputFileName(), numberOfOperations);
                }
                case ReceivingSubscriber s -> {
                    task = new MeasurementRequesterSubscriberTask(s, experimentRun, getInputFileName(), numberOfOperations);
                }
                default -> {
                    return;
                }
            }
            method.accept(task);
        }
        
        
        public void addPreTask(BlindingEntity entity, List<String> domains) {
            addTaskHelper(entity, domains, (task) -> { experimentRun.addPreTask(task); });
        }
        
        public void addTask(BlindingEntity entity, List<String> domains) {
            addTaskHelper(entity, domains, (task) -> { experimentRun.addTask(task); });
        }
        
        public void addPostTask(BlindingEntity entity, List<String> domains) { 
            addTaskHelper(entity, domains, (task) -> { experimentRun.addPostTask(task); });
        }
        
        private void addTaskHelper(BlindingEntity entity, List<String> domains, Consumer<AsynchronousTask> method) {
            AsynchronousTask task;
            switch (entity) {
                case Publisher p -> {
                    task = new MeasurementRequesterPublisherTask(p, experimentRun, domains);
                }
                case ReceivingSubscriber s -> {
                    task = new MeasurementRequesterSubscriberTask(s, experimentRun, domains);
                }
                default -> {
                    return;
                }
            }
            method.accept(task);
        }
        
        public void addRunCleaningTasks() {
            
            boolean subTaskFound = false;
            boolean pubTaskFound = false;
            
            for (Task t : experimentRun.getPostTasks()) {
                switch (t) {
                    case SubscriberTask s -> subTaskFound = true;
                    case PublisherTask p -> pubTaskFound = true;
                    default -> {
                    }
                } 
            }
            
            if (!subTaskFound) {
                experimentRun.addPostTask(new SubscriberNopTask(new ReceivingSubscriber("sub"), experimentRun));
            }
            
            if (!pubTaskFound) {
                experimentRun.addPostTask(new PublisherNopTask(new Publisher("pub"), experimentRun));
            }
        }
    }
}
