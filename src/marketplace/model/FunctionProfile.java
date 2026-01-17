package marketplace.model;

import java.util.Map;

public class FunctionProfile {
    // The abstract identity (e.g., "medical-analysis")
    private final String serviceName;
    // The code artifact (e.g., "docker.io/med:v1")
    private final String dockerImage; 
    // What the code needs (e.g., "min_ram=4GB")
    private final Map<String, Object> requirements;

    public FunctionProfile(String serviceName, String dockerImage, Map<String, Object> requirements) {
        this.serviceName = serviceName;
        this.dockerImage = dockerImage;
        this.requirements = requirements;
    }

    public String getServiceName() { return serviceName; }
    public String getDockerImage() { return dockerImage; }
    public Map<String, Object> getRequirements() { return requirements; }
}