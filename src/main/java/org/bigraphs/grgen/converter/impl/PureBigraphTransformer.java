package org.bigraphs.grgen.converter.impl;

import com.google.common.graph.Traverser;
import org.bigraphs.framework.core.Bigraph;
import org.bigraphs.framework.core.BigraphEntityType;
import org.bigraphs.framework.core.impl.BigraphEntity;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.signature.DefaultDynamicSignature;
import org.bigraphs.grgen.converter.BigraphTransformer;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.bigraphs.framework.core.BigraphMetaModelConstants.*;

/**
 * This class transforms an Ecore bigraph instance model (i.e., a graph typed over a type graph extended by a signature) to GrGen.NET's graph format.
 * <p>
 * GrGen.NET's graph metamodel represents essentially a control-compatible extension of the basic type graph of bigraphs (refer to [KeTG16]).
 * <p>
 * The bigraph must have special properties: It must be prime and ground.
 * Such a bigraph is used as an agent for a bigraphical reactive system (BRS).
 *
 * @author Dominik Grzelak
 * @see <a href="http://arxiv.org/abs/1612.01638">[KeTG16] Kehrer, Timo ; Tsigkanos, Christos ; Ghezzi, Carlo: An EMOF-Compliant Abstract Syntax for Bigraphs. In: Electronic Proceedings in Theoretical Computer Science Bd. 231 (2016), S. 16–30. — arXiv: 1612.01638</a>
 */
public class PureBigraphTransformer extends BigraphTransformer {

    private boolean withOppositeEdges = false;

    public PureBigraphTransformer withOppositeEdges(boolean flag) {
        this.withOppositeEdges = flag;
        return this;
    }

    @Override
    public String toString(Bigraph bigraph) {
        assertCorrectClassType(PureBigraph.class, bigraph);
        assertPrimeAndGround((PureBigraph) bigraph);

        StringBuilder sb = new StringBuilder();
        PureBigraph pureBigraph = (PureBigraph) bigraph;

        // (1) Loop through the node hierarchy using a BFS
        // Create all node instances first and ports.
        // Also establish parent-child relations
        // Inner names/sites are not important and forbidden anyway here
        // Store in a map: edge/outer -> port of node (for the second run)

        Map<BigraphEntity.Link, LinkedHashSet<BigraphEntity.Port>> linkPortMap = new HashMap<>();
        Map<BigraphEntity.Link, String> linkNameMap = new HashMap<>();
        Map<BigraphEntity.Port, String> portNameMap = new HashMap<>();
        Supplier<String> nodeNameSupplier = createNameSupplier("node");
        Supplier<String> portNameSupplier = createNameSupplier("p");
        Supplier<String> linkNameSupplier = createNameSupplier("link");
        Traverser<BigraphEntity> traverser = Traverser.forTree(x -> {
            List<BigraphEntity<?>> children = pureBigraph.getChildrenOf(x);
//            System.out.format("    %s has %d children\n", x.getType(), children.size());
            return children;
        });

        // Run BFS and for each bigraph node create graph nodes and their port nodes
        // Here we also collect all edges/outer names involved
        Iterable<BigraphEntity> bigraphEntities = traverser.breadthFirst(bigraph.getRoots());
        sb.append(LINE_SEP).append("# Place Graph").append(LINE_SEP);
        bigraphEntities.forEach(x -> {
//            System.out.println(x);
            if (BigraphEntityType.isRoot(x)) {
                sb.append(createNodeInstance(CLASS_ROOT, "root", "r0", 0));
                sb.append(LINE_SEP);
            }
            if (BigraphEntityType.isNode(x)) {
                BigraphEntity.NodeEntity nodeEntity = (BigraphEntity.NodeEntity) x;
                sb.append(createNodeInstance(nodeEntity.getControl().getNamedType().stringValue(), nodeNameSupplier.get(), nodeEntity.getName()));
                sb.append(LINE_SEP);

                pureBigraph.getPorts(nodeEntity)
                        .forEach(p -> {
                            BigraphEntity.Link link = pureBigraph.getLinkOfPoint(p);
                            if (link != null) {
                                if (!linkPortMap.containsKey(link)) {
                                    linkPortMap.put(link, new LinkedHashSet<>());
                                }
                                linkPortMap.get(link).add(p);
                            }
                        });

                // Create as many Port nodes wrt the control's arity
                for (int i = 0; i < nodeEntity.getControl().getArity().getValue().intValue(); i++) {
                    String portVar = portNameSupplier.get();
                    portNameMap.put(pureBigraph.getPorts(nodeEntity).get(i), portVar);
                    String portName = "port" + portVar.replaceFirst("p", "");
                    sb.append(createNodeInstance(CLASS_PORT, portName, portVar, i));
                    sb.append(LINE_SEP);
                    sb.append(
                            createEdge(REFERENCE_NODE,
                                    "",
                                    portVar,
                                    nodeEntity.getName()
                            ));
                    sb.append(LINE_SEP);

                    if (withOppositeEdges) {
                        sb.append(
                                createEdge(REFERENCE_PORT,
                                        "",
                                        nodeEntity.getName(),
                                        portVar
                                ));
                    }
                    sb.append(LINE_SEP);
                }
            }
        });

        // Rerun BFS again to create the parent-child relations now that we know we can access the node variables
        Iterable<BigraphEntity> bigraphEntities2 = traverser.breadthFirst(bigraph.getRoots());
        bigraphEntities2.forEach(x -> {
            String nodeVar = "";
            if (BigraphEntityType.isRoot(x)) {
                nodeVar = "r0";
            }
            if (BigraphEntityType.isNode(x)) {
                BigraphEntity.NodeEntity nodeEntity = (BigraphEntity.NodeEntity) x;
                nodeVar = nodeEntity.getName();
            }
            // Create opposite edges bPrnt,bChilds
            for (BigraphEntity<?> each : pureBigraph.getChildrenOf(x)) {
                if (BigraphEntityType.isNode(each)) {
                    sb.append(
                            createEdge(REFERENCE_PARENT,
                                    "",
                                    ((BigraphEntity.NodeEntity) each).getName(),
                                    nodeVar
                            ));
                    sb.append(LINE_SEP);

                    if (withOppositeEdges) {
                        sb.append(
                                createEdge(REFERENCE_CHILD,
                                        "",
                                        nodeVar,
                                        ((BigraphEntity.NodeEntity) each).getName()
                                ));
                        sb.append(LINE_SEP);
                    }
                }
            }
        });

        // (2) Re-construct the hypergraph linkage
        // Use the edge/outer map created before and create respective nodes of type "BLink"
        // All relevant nodes shall be created until now
        Set<Map.Entry<BigraphEntity.Link, LinkedHashSet<BigraphEntity.Port>>> entries = linkPortMap.entrySet();
        if (!entries.isEmpty()) {
            sb.append(LINE_SEP).append("# Link Graph").append(LINE_SEP);
        }
        for (Map.Entry<BigraphEntity.Link, LinkedHashSet<BigraphEntity.Port>> each : entries) {
            BigraphEntity.Link linkElem = each.getKey();
            Set<BigraphEntity.Port> ports = each.getValue();

            String nodeType = BigraphEntityType.isEdge(linkElem) ? CLASS_EDGE : CLASS_OUTERNAME;
            String nodeVar = linkElem.getName();
            String nodeName = linkNameSupplier.get();

            linkNameMap.put(linkElem, nodeVar);
            sb.append(createNodeInstance(nodeType, nodeName, nodeVar)).append(LINE_SEP);
            // Create opposite edges bLink,bPoints
            for (BigraphEntity.Port p : ports) {
                String portVar = portNameMap.get(p);
                sb.append(
                        createEdge(REFERENCE_LINK,
                                "",
                                portVar,
                                nodeVar
                        ));
                sb.append(LINE_SEP);

                if (withOppositeEdges) {
                    sb.append(
                            createEdge(REFERENCE_POINT,
                                    "",
                                    nodeVar,
                                    portVar
                            ));
                    sb.append(LINE_SEP);
                }
            }
        }

        List<BigraphEntity.OuterName> ol = new ArrayList<>(pureBigraph.getOuterNames());
        ol.removeAll(entries.stream().map(x -> x.getKey()).collect(Collectors.toList()));
        for (BigraphEntity.OuterName each : ol) {
            sb.append(createNodeInstance(CLASS_OUTERNAME, each.getName(), each.getName())).append(LINE_SEP);
        }
        // Last: idle edges and idle outer names

        return sb.toString();
    }
}
