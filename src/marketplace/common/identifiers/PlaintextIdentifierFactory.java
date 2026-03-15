package marketplace.common.identifiers;

/**
 * Baseline strategy: Wraps the ID in plaintext with zero cryptographic overhead.
 */
public class PlaintextIdentifierFactory implements RoutingIdentifierFactory {
    @Override
    public ServiceIdentifier createIdentifier(long oracleId) {
        return new PlaintextIdentifier(oracleId);
    }
}