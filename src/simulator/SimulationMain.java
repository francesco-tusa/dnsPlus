package simulator;

import simulator.simulations.RegionFileBasedSimulation;
import simulator.simulations.RegionFixedSimulation;
import simulator.simulations.RegionRandomSimulation;


/**
 * Main entry point for running simulations.
 * Delegates execution to a specific SimulationRunner implementation.
 */
public class SimulationMain {

    public static void main(String[] args) {
        RegionFileBasedSimulation.main(args);
        //RegionFixedSimulation.main(args);
        //RegionRandomSimulation.main(args);
    }
}