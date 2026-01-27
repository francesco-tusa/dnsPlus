package simulator.topology.factories;

import simulator.entities.SimulationBroker;

public interface BrokerFactory {
    SimulationBroker createBroker(String name);
    SimulationBroker createLeafBroker(String name);
}