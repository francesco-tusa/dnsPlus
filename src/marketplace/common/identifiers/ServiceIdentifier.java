package marketplace.common.identifiers;

/**
 * A generic marker interface representing a function's routing identifier.
 * The underlying structure depends entirely on the active cryptographic substrate.
 */
public interface ServiceIdentifier {
    
    // Optional: Useful for runtime validation or logging
    String getSchemeName(); 
}