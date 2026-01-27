package simulator.core;

/**
 * Interface for entities that provide location information for simulation logging.
 * Allows polymorphic retrieval of loggable location strings and physical metric locations.
 */
public interface LoggableEntity {

    /**
     * Determines what location string should be logged in the trace file
     * when this entity is the SOURCE of a message.
     * * @param receiverLocationInfo The location/region string of the node receiving the message.
     * @return The string to be logged in the "Location" column.
     */
    String resolveLogLocation(String receiverLocationInfo);

    /**
     * Returns the actual physical location of this entity if it exists.
     * Used for populating summary files (e.g., identifying the country/coordinates of origin).
     * * @return The Location object, or null if this entity does not have a physical location (e.g., abstract Brokers).
     */
    Location getMetricLocation();
}