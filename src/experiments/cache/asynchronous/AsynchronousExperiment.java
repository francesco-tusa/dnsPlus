package experiments.cache.asynchronous;

import experiments.Experiment;
import experiments.outputdata.AsynchronousExperimentStatsCalculator;

/**
 *
 * @author uceeftu
 */
public abstract class AsynchronousExperiment extends Experiment {

    public AsynchronousExperiment(String name, String inputFileName, int numberOfRuns) {
        super(name, inputFileName, numberOfRuns, new AsynchronousExperimentStatsCalculator(name));
    }
}
