package simulator.topology.geonames.builder;

import java.util.ArrayList;
import java.util.List;
import simulator.regions.Region;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeoNamesBuilderNode {
    public int geonameId;
    public String name;
    public String featureCode;
    public String code;
    public NodeType type;
    public long aggregatedPopulation;
    public long internetPopulation;
    public Long officialPopulation;
    public Double internetPenetrationRate;
    public Region bounds;
    public List<GeoNamesBuilderNode> children = new ArrayList<>();

    public enum NodeType {
        WORLD, CONTINENT, COUNTRY, ADM1, ADM2, PPL,
        GRID_COARSE,  // Large-scale partitions (e.g., > 1M people)
        GRID_FINE     // Fine-grained partitions (e.g., > 10k people)

    }

    public GeoNamesBuilderNode() {}

    public GeoNamesBuilderNode(int geonameId, String name, NodeType type, String code, String featureCode) {
        this.geonameId = geonameId;
        this.name = name;
        this.type = type;
        this.code = (code != null) ? code : "";
        this.featureCode = (featureCode != null) ? featureCode : "";
    }
    
    // Copy constructor for subsets
    public GeoNamesBuilderNode(GeoNamesBuilderNode other) {
        this.geonameId = other.geonameId;
        this.name = other.name;
        this.featureCode = other.featureCode;
        this.code = other.code;
        this.type = other.type;
        this.aggregatedPopulation = other.aggregatedPopulation;
        this.internetPopulation = other.internetPopulation;
        this.officialPopulation = other.officialPopulation;
        this.internetPenetrationRate = other.internetPenetrationRate;
        if (other.bounds != null) this.bounds = new Region(other.bounds);
        this.children = new ArrayList<>();
    }

    public void addChild(GeoNamesBuilderNode child) {
        if (child != null) children.add(child);
    }
}