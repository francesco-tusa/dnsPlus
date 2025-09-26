package simulator.visualisation;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Edge;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.Viewer;
import simulator.core.TreeNode;
import simulator.entities.PublisherWithLocation;
import simulator.entities.SubscriberWithLocation;
import simulator.events.PublicationWithLocation;
import simulator.events.SimulationSubscription;
import simulator.events.SubscriptionWithLocation;
import simulator.regions.BrokerWithRegion;
import simulator.regions.SubscriptionWithRegion;

public class TopologyVisualiser {

    private static TopologyVisualiser instance;
    private final Graph graph;

    private final String stylesheet =
        "node { text-alignment: at-right; text-offset: 10px, 0px; text-color: #333; text-size: 14; text-style: normal; }" +
        "node.broker { fill-color: #3498DB; size: 14px; stroke-mode: plain; stroke-color: #2874A6; }" +
        "node.subscriber { fill-color: #58D68D; shape: box; size: 12px, 12px; stroke-mode: plain; stroke-color: #239B56; }" +
        "node.publisher { fill-color: #F1C40F; shape: diamond; size: 16px; stroke-mode: plain; stroke-color: #B7950B; }" +
        "edge { fill-color: #AEB6BF; arrow-shape: none; size: 2px; text-alignment: under; text-size: 12; text-color: #444; text-style: normal; text-background-mode: rounded-box; text-background-color: white; text-padding: 3px; }" +
        "edge.subscription { fill-color: #2ECC71; size: 3px; }" +
        "edge.publication { fill-color: #E74C3C; size: 3px; }" +
        "edge.bipath { fill-color: #8E44AD; size: 3px; }";

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
        graph.display().setCloseFramePolicy(Viewer.CloseFramePolicy.EXIT);
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
            edge.setAttribute("up_sub_count", 0);
            edge.setAttribute("down_sub_count", 0);
            edge.setAttribute("up_pub_count", 0);
            edge.setAttribute("down_pub_count", 0);
            edge.setAttribute("has_subscription", false);
            edge.setAttribute("has_publication", false);
        }
    }

    public void updateSubscriptionEdge(String from, String to, boolean isUpward) {
        Edge edge = findEdge(from, to);
        if (edge != null) {
            String attribute = isUpward ? "up_sub_count" : "down_sub_count";
            int currentCount = edge.getAttribute(attribute, Integer.class);
            edge.setAttribute(attribute, currentCount + 1);
            edge.setAttribute("has_subscription", true);
            updateEdgeStyle(edge);
            updateEdgeLabel(edge);
        }
    }

    public void updatePublicationEdge(String from, String to, boolean isUpward) {
        Edge edge = findEdge(from, to);
        if (edge != null) {
            String attribute = isUpward ? "up_pub_count" : "down_pub_count";
            int currentCount = edge.getAttribute(attribute, Integer.class);
            edge.setAttribute(attribute, currentCount + 1);
            edge.setAttribute("has_publication", true);
            updateEdgeStyle(edge);
            updateEdgeLabel(edge);
        }
    }

    public void setNodeActive(String nodeName) {
        Node node = graph.getNode(nodeName);
        if (node != null) {
            node.setAttribute("ui.style", "stroke-width: 3px; text-style: bold;");
        }
    }
    
    public void updateSubscriberLabel(SubscriberWithLocation subscriber, SimulationSubscription subscription) {
        if (subscriber == null || subscription == null) return;
        
        Node node = graph.getNode(subscriber.getName());
        if (node != null) {
            String label = subscriber.getName();
            if (subscription instanceof SubscriptionWithRegion subRegion) {
                label += " " + subRegion.getRegion().toShortString();
            } else if (subscription instanceof SubscriptionWithLocation subLocation) {
                label += " " + subLocation.getLocation().toString();
            }
            node.setAttribute("ui.label", label);
        }
    }

    /**
     * A new method to update the label of a publisher node to display
     * the location of the publication it is sending.
     * @param publisher The publisher node whose label is to be updated.
     * @param publication The publication containing the location to display.
     */
    public void updatePublisherLabel(PublisherWithLocation publisher, PublicationWithLocation publication) {
        if (publisher == null || publication == null) return;
        
        Node node = graph.getNode(publisher.getName());
        if (node != null) {
            String label = String.format("%s %s", publisher.getName(), publication.getLocation().toShortString());
            node.setAttribute("ui.label", label);
        }
    }

    private void updateEdgeStyle(Edge edge) {
        boolean hasSub = edge.getAttribute("has_subscription", Boolean.class);
        boolean hasPub = edge.getAttribute("has_publication", Boolean.class);

        if (hasSub && hasPub) {
            edge.setAttribute("ui.class", "bipath");
        } else if (hasSub) {
            edge.setAttribute("ui.class", "subscription");
        } else if (hasPub) {
            edge.setAttribute("ui.class", "publication");
        }
    }

    private void updateEdgeLabel(Edge edge) {
        int upSub = edge.getAttribute("up_sub_count", Integer.class);
        int downSub = edge.getAttribute("down_sub_count", Integer.class);
        int upPub = edge.getAttribute("up_pub_count", Integer.class);
        int downPub = edge.getAttribute("down_pub_count", Integer.class);

        int totalSub = upSub + downSub;
        int totalPub = upPub + downPub;

        StringBuilder label = new StringBuilder();

        if (totalSub > 0) {
            label.append(String.format("S:%d(%d,%d)", totalSub, upSub, downSub));
        }

        if (totalPub > 0) {
            if (label.length() > 0) {
                label.append(" ");
            }
            label.append(String.format("P:%d(%d,%d)", totalPub, upPub, downPub));
        }

        if (label.length() > 0) {
            edge.setAttribute("ui.label", label.toString().trim());
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