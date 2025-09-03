package simulator.visualisation;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Edge;
import org.graphstream.graph.implementations.SingleGraph;

import simulator.core.TreeNode;
import simulator.regions.BrokerWithRegion;

public class TopologyVisualiser {

    private static TopologyVisualiser instance;
    private final Graph graph;

    // The stylesheet is updated to explicitly enable and style labels on ALL edge types.
    private final String stylesheet =
        "node { text-alignment: at-right; text-offset: 10px, 0px; text-color: #333; text-size: 14; text-style: bold; }" +
        "node.broker { fill-color: #3498DB; size: 14px; stroke-mode: plain; stroke-color: #2874A6; }" +
        "node.subscriber { fill-color: #58D68D; shape: box; size: 12px, 12px; stroke-mode: plain; stroke-color: #239B56; }" +
        "node.publisher { fill-color: #F1C40F; shape: diamond; size: 16px; stroke-mode: plain; stroke-color: #B7950B; }" +
        // --- THIS IS THE KEY FIX ---
        // All edges, including those with a specific class, will now have these text properties.
        "edge { fill-color: #AEB6BF; arrow-shape: none; size: 2px; text-size: 14; text-color: #444; text-style: bold; text-background-mode: rounded-box; text-background-color: white; text-padding: 3px; }" +
        "edge.subscription { fill-color: #2ECC71; size: 3px; }" +
        "edge.publication { fill-color: #E74C3C; size: 3px; }";

    private TopologyVisualiser() {
        System.setProperty("org.graphstream.ui", "swing");
        graph = new SingleGraph("Topology");
        graph.setAttribute("ui.stylesheet", stylesheet);
        graph.setAttribute("ui.quality");
        graph.setAttribute("ui.antialias");
    }

    public static synchronized TopologyVisualiser getInstance() {
        if (instance == null) {
            instance = new TopologyVisualiser();
        }
        return instance;
    }

    public void displayGraph() {
        graph.display().setCloseFramePolicy(org.graphstream.ui.view.Viewer.CloseFramePolicy.EXIT);
    }

    public void addNode(TreeNode treeNode) {
        if (graph.getNode(treeNode.getName()) == null) {
            Node node = graph.addNode(treeNode.getName());
            String label = treeNode.getName();
            if (treeNode instanceof BrokerWithRegion) {
                BrokerWithRegion br = (BrokerWithRegion) treeNode;
                label += " " + br.getRegion().toShortString();
            }
            node.setAttribute("ui.label", label);

            if (treeNode instanceof simulator.entities.SubscriberWithLocation) {
                node.setAttribute("ui.class", "subscriber");
            } else if (treeNode instanceof simulator.entities.PublisherWithLocation) {
                node.setAttribute("ui.class", "publisher");
            } else {
                node.setAttribute("ui.class", "broker");
            }
        }
    }

    public void addEdge(String from, String to) {
        String edgeId = from + "--" + to;
        if (graph.getEdge(edgeId) == null) {
            Edge edge = graph.addEdge(edgeId, from, to);
            edge.setAttribute("subscription_count", 0);
        }
    }

    public void updateSubscriptionEdge(String from, String to) {
        Edge edge = findEdge(from, to);
        if (edge != null) {
            int currentCount = edge.getAttribute("subscription_count", Integer.class);
            currentCount++;
            edge.setAttribute("subscription_count", currentCount);
            
            edge.setAttribute("ui.class", "subscription");
            edge.setAttribute("ui.label", "" + currentCount);
        }
    }

    public void setPublicationEdge(String from, String to) {
        Edge edge = findEdge(from, to);
        if (edge != null) {
            int currentCount = edge.getAttribute("subscription_count", Integer.class);
            
            // Set the color to red, but also set the label with the final count
            edge.setAttribute("ui.class", "publication");
            edge.setAttribute("ui.label", "" + currentCount);
        }
    }
    
    private Edge findEdge(String from, String to) {
        Edge edge = graph.getEdge(from + "--" + to);
        if (edge == null) {
            edge = graph.getEdge(to + "--" + from);
        }
        return edge;
    }
}
