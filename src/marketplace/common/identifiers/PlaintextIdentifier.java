package marketplace.common.identifiers;

public class PlaintextIdentifier implements ServiceIdentifier {
    private final long functionId;

    public PlaintextIdentifier(long functionId) {
        this.functionId = functionId;
    }

    public long getId() { return functionId; }

    @Override
    public String getSchemeName() { return "PLAINTEXT"; }
}