package org.example.impl;

import com.google.common.graph.Traverser;
import org.bigraphs.framework.core.BigraphEntityType;
import org.bigraphs.framework.core.impl.BigraphEntity;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.signature.DefaultDynamicControl;
import org.bigraphs.framework.core.impl.signature.DefaultDynamicSignature;
import org.bigraphs.framework.core.reactivesystem.HasLabel;
import org.bigraphs.framework.core.reactivesystem.ReactionRule;
import org.example.RuleTransformer;

import java.util.*;
import java.util.function.Supplier;

import static org.bigraphs.framework.core.BigraphMetaModelConstants.*;

//TODO extend from GroundRuleTransformer - hook method for NACs

/**
 * A concrete transformer implementation for parametrized rules that take pure bigraphs as redex and reactum.
 * A rule is parametrized if it has sites specified.
 *
 * @author Dominik Grzelak
 */
public class PureParametrizedRuleTransformer extends RuleTransformer {
    PureBigraphTransformer bigraphTransformer = new PureBigraphTransformer();


    @Override
    public String toString(ReactionRule<?> element) {
        assertCorrectClassType(PureBigraph.class, element.getRedex());
        assertCorrectClassType(PureBigraph.class, element.getReactum());
        PureBigraph redex = (PureBigraph) element.getRedex();
        PureBigraph reactum = (PureBigraph) element.getReactum();

        StringBuilder sb = new StringBuilder();
        Supplier<String> patternNameSupplier = createNameSupplier("pat");
        // we also need to refer to some edge variables later for bPrnt-typed relations
        Supplier<String> edgeSupplier = createNameSupplier("c");
        Map<String, String> edgesDeclared = new LinkedHashMap<>();
        Map<BigraphEntity.Link, LinkedHashSet<BigraphEntity.Port>> linkPortMap = new HashMap<>();
        Map<BigraphEntity.Link, LinkedHashSet<BigraphEntity.Port>> linkPortMapRewrite = new HashMap<>();
        Map<String, BigraphEntity> varMap = new HashMap<>(); // map from variable name to NodeEntity
        Set<BigraphEntity> newNodesAdded = new LinkedHashSet<>(); // a set of all nodes, that are added in the reactum not present in the redex (thus, also not mapped in the tracking map)
        Map<String, String> redexSiteToReactumSite = new HashMap<>(); // the parent element, a node, is stored where the site is located
        // for nodes without sites: a NAC pattern is called upon them.
        // They must contain as many nodes via bPrnt as specified in the rule. Only then the rule respects the host graph
        List<BigraphEntity.NodeEntity> nodesWithoutSites = new LinkedList<>();

        String ruleName = element instanceof HasLabel && ((HasLabel) element).getLabel() != null ? ((HasLabel) element).getLabel() : "ruleName";

        // Open rule declaration
        sb.append(openRule(ruleName));

        // Create a list of variables for the root, every control in the redex, and node ports
        redex.getRoots().forEach(x -> {
            String varName = "r" + x.getIndex();
            if (!varMap.containsKey(varName)) {
                // For multiary interfaces in bigraphs (multiple roots): every child of r1,r2,... is mapped to the r0 node.
//                String varName = "r0";
//            String variableFor = createVarTypeDeclWithComma(varName, CLASS_ROOT);
//            String variableFor = createVarTypeDeclWithComma(varName, CLASS_NODE);
                // "Node" is a generic GrGen class type
                String variableFor = createVarTypeDeclWithComma(varName, "Node"); // because the rules root is a placeholder for any node
                sb.append(variableFor).append(DEFAULT_WHITESPACE_CHAR);

                varMap.put(varName, x);
            }
        });
        StringBuilder portAttributeChecks = new StringBuilder();
        redex.getNodes().stream().forEach(x -> {
            String varName = x.getName(); //nameSupplier.get();
            String variableFor = createVarTypeDeclWithComma(varName, x.getControl());
            sb.append(variableFor).append(DEFAULT_WHITESPACE_CHAR);
            varMap.put(varName, x);

            // Nodes without sites: a NAC pattern will be called upon them
            if (!redex.getChildrenOf(x).stream().anyMatch(BigraphEntityType::isSite)) {
                nodesWithoutSites.add(x);
            }

            // We also need to check the port's index
            // "Attributes of graph elements can be checked within if clauses" [GrGen-Manual]
            redex.getPorts(x)
                    .forEach(p -> {
                        String varNameOfPort = x.getName() + "_p" + p.getIndex();
                        String variableForPort = createVarTypeDeclWithComma(varNameOfPort, CLASS_PORT);
                        sb.append(variableForPort).append(DEFAULT_WHITESPACE_CHAR);
                        portAttributeChecks.append(String.format("if { %s.ix == %d; }", varNameOfPort, p.getIndex())).append(LINE_SEP);
                    });
        });
        sb.append(LINE_SEP);

        // Rebuild place graph structure from redex (parent-child relationship
        Traverser<BigraphEntity> traverser = Traverser.forTree(x -> redex.getChildrenOf(x));
        Iterable<BigraphEntity> bigraphEntities2 = traverser.breadthFirst(redex.getRoots());
        bigraphEntities2.forEach(x -> {
            String nodeVar = "";
            if (BigraphEntityType.isRoot(x)) {
                // Having multiple roots in a bigraph rule is default in GrGen.
//                nodeVar = "r0"; // Use always r0 since the agent is always prime
                nodeVar = "r" + ((BigraphEntity.RootEntity)x).getIndex();
            }
            if (BigraphEntityType.isNode(x)) {
                BigraphEntity.NodeEntity nodeEntity = (BigraphEntity.NodeEntity) x;
                nodeVar = nodeEntity.getName();

                // Create port->node reference bNode
                String finalNodeVar = nodeVar;
                redex.getPorts(nodeEntity).forEach(p -> {
                    String edgePrefix = edgeSupplier.get();
                    String edgeVar = createVarTypeDecl(edgePrefix, REFERENCE_NODE);
                    edgesDeclared.put(finalNodeVar + "_p" + p.getIndex(), edgePrefix);
                    String portNodeRel = createGraphlet(finalNodeVar + "_p" + p.getIndex(), edgeVar, finalNodeVar);
                    sb.append(portNodeRel).append(LINE_SEP);
                    // Map link to set of ports for later
                    BigraphEntity.Link link = redex.getLinkOfPoint(p);
                    if (link != null) {
                        if (!linkPortMap.containsKey(link)) {
                            linkPortMap.put(link, new LinkedHashSet<>());
                        }
                        linkPortMap.get(link).add(p);
                    }
                });
            }

            // Create opposite edges bPrnt,bChilds
            for (BigraphEntity<?> each : redex.getChildrenOf(x)) {
                if (BigraphEntityType.isNode(each)) {

                    String edgePrefix = edgeSupplier.get();
                    String edgeVar = createVarTypeDecl(edgePrefix, REFERENCE_PARENT);
                    edgesDeclared.put(((BigraphEntity.NodeEntity) each).getName(), edgePrefix);

                    //every first-level child under any root gets linked to r0
                    sb.append(
                            createGraphlet(((BigraphEntity.NodeEntity) each).getName(), edgeVar, nodeVar)
                    );
                    sb.append(LINE_SEP);
                }
            }
        });

        // if we have nodes specified without sites, we have to add NAC
        // That express that also the host graph must contain the exact number of specified children
        if (nodesWithoutSites.size() != 0) {
            // first we append the GrGen pattern "function" that is reused before the rule definition
            if(isPrintNACPattern()) {
                sb.insert(0, TEMPLATE_NAC_NO_SITE_PATTERN + LINE_SEP + LINE_SEP);
            }
            nodesWithoutSites.forEach(x -> {
                x.getName();
                sb.append(patternNameSupplier.get()).append(":nac_NodesWithoutSites(")
                        .append(x.getName()).append(", ")
                        .append(redex.getChildrenOf(x).size())
                        .append(");").append(LINE_SEP);
            });
        }

        // Add the if-checks for the port indices
        sb.append(portAttributeChecks);

        // Rebuild link graph structure from redex
        // Connect ports to links
        // Here we do not need to supply edge variables names (we dont need to refer to them)
        sb.append(LINE_SEP);
        Set<Map.Entry<BigraphEntity.Link, LinkedHashSet<BigraphEntity.Port>>> entries = linkPortMap.entrySet();
        for (Map.Entry<BigraphEntity.Link, LinkedHashSet<BigraphEntity.Port>> each : entries) {
            LinkedHashSet<BigraphEntity.Port> ports = each.getValue();
            BigraphEntity.Link link = each.getKey();
            // we have to use this generic type, since for bigraph matching an outer name in a rule can mean anything. coule also be an edge.
            // So if it is explicitly an edge, then use the edge class, but if its an outername use the generic type BLink
            String nodeType = BigraphEntityType.isEdge(link) ? CLASS_EDGE : CLASS_LINK; //CLASS_OUTERNAME;
            String variableForLink = createVarTypeDeclWithComma(link.getName(), nodeType);
            sb.append(variableForLink).append(LINE_SEP);
            for (BigraphEntity.Port eachPort : ports) {
                BigraphEntity.NodeEntity<DefaultDynamicControl> nodeOfPort = redex.getNodeOfPort(eachPort);
                String portVarName = nodeOfPort.getName() + "_p" + eachPort.getIndex();
                String portLinkRel = createGraphlet(portVarName, createVarTypeDecl("", REFERENCE_LINK), link.getName());
                sb.append(portLinkRel);
                sb.append(LINE_SEP);
            }
        }
        // Idle outernames in the redex
        redex.getOuterNames().stream().filter(x -> redex.getPointsFromLink(x).isEmpty()).forEach(idleLink -> {
            sb.append(createVarTypeDeclWithComma(idleLink.getName(), CLASS_OUTERNAME)).append(LINE_SEP);
        });

        sb.append(openRuleReplaceBlock());
        sb.append(LINE_SEP);
        // We cannot use the reactum's node/edges directly. They might have different identifiers.
        // Use the map to resolve names of the reactum that are coming from the redex
        // Elements not mapped get deleted (when not "mapped/specified" in the rule's rewrite part -> automatic deletion in the replace mode in GrGen)

        // We add all nodes in the tracking map without image (reactum |-> redex is empty)

//        System.out.println(trackingMap.size());
//        System.out.println(redex.getNodes().size());
//        System.out.println(reactum.getNodes().size());
        //TODO throw exception (as long as no instantiation maps are supported)
        assert redex.getSites().size() >= reactum.getSites().size();

        // Clear the port index checks ad re-build for the reactum
        portAttributeChecks.setLength(0);

        // Place graph part for the rewrite pattern
        for (Map.Entry<String, String> each : trackingMap.entrySet()) {
            String reactumId = each.getKey();
            String redexId = each.getValue();

            if (trackingMap.isLink(redexId) || trackingMap.isLink(reactumId)) continue;

            // Handle here the addition of new nodes, the mapping has no image
            if (redexId == null || redexId.isEmpty()) {
                BigraphEntity.NodeEntity<DefaultDynamicControl> rhsNode = getNodeById(reactumId, reactum).get();
                newNodesAdded.add(rhsNode);
                String rhsParentId = getParentId(rhsNode, reactum, true);
                String edgePrefix = createVarTypeDecl("", REFERENCE_PARENT);
                // Since this node is new; we need to declare it first
                sb.append(createVarTypeDeclWithComma(reactumId, rhsNode.getControl())).append(LINE_SEP);
                sb.append(createGraphlet(reactumId, edgePrefix, rhsParentId));
                sb.append(LINE_SEP);
                // Also the new node's ports
                reactum.getPorts(rhsNode).forEach(p -> {
                    String varNameOfPort = reactumId + "_p" + p.getIndex();
                    String variableForPort = createVarTypeDeclWithComma(varNameOfPort, CLASS_PORT);
                    sb.append(variableForPort).append(LINE_SEP);
                    String edgePrefix2 = edgeSupplier.get();
                    String edgeVar = createVarTypeDecl(edgePrefix2, REFERENCE_NODE);
                    edgesDeclared.put(varNameOfPort, edgePrefix2);
                    String portNodeRel = createGraphlet(
                            varNameOfPort,
                            edgeVar,
                            reactumId
                    );
                    sb.append(portNodeRel).append(LINE_SEP);
//                    portAttributeChecks.append(String.format("if { %s.ix == %d; }", varNameOfPort, p.getIndex())).append(LINE_SEP);
                    // Map the port to the link for later
                    BigraphEntity.Link link = reactum.getLinkOfPoint(p);
                    if (link != null) {
                        if (!linkPortMapRewrite.containsKey(link)) {
                            linkPortMapRewrite.put(link, new LinkedHashSet<>());
                        }
                        linkPortMapRewrite.get(link).add(p);
                    }
                });
                continue;
            }

            // TODO These could be empty when the tracking map is ill-formed
            BigraphEntity.NodeEntity<DefaultDynamicControl> lhsNode = getNodeById(redexId, redex).get();
            BigraphEntity.NodeEntity<DefaultDynamicControl> rhsNode = getNodeById(reactumId, reactum).get();
            String lhsParentId = getParentId(lhsNode, redex, false);
            String rhsParentId = getParentId(rhsNode, reactum, true);

            String edgePrefix = edgesDeclared.get(redexId);
            if (!rhsParentId.equalsIgnoreCase(lhsParentId)) {
//                System.out.println("Element moved!");
                // we cannot reuse an edge variable when it does not connect the same nodes
                // so we create an anonymous edge class of the same type as before
                edgePrefix = createVarTypeDecl("", REFERENCE_PARENT);
            }
            sb.append(createGraphlet(redexId, edgePrefix, rhsParentId));
            sb.append(LINE_SEP);

            reactum.getPorts(rhsNode).forEach(p -> {
                String varNameOfPort = redexId + "_p" + p.getIndex();
                String portNodeRel = createGraphlet(varNameOfPort, edgesDeclared.get(varNameOfPort), redexId);
                sb.append(portNodeRel).append(LINE_SEP);

//                portAttributeChecks.append(String.format("if { %s.ix == %d; }", varNameOfPort, p.getIndex())).append(LINE_SEP);
                // Map link to set of ports for later
                BigraphEntity.Link link = reactum.getLinkOfPoint(p);
                if (link != null) {
                    if (!linkPortMapRewrite.containsKey(link)) {
                        linkPortMapRewrite.put(link, new LinkedHashSet<>());
                    }
                    linkPortMapRewrite.get(link).add(p);
                }
            });
        }

//        sb.append(portAttributeChecks).append(LINE_SEP);

        // Link graph part for the rewrite pattern
        //Handling new edge/outername additions? Not necessary: Outernames cannot be added by the reactum so there will not be a problem, all outernames have to be specified already in the redex.
        Set<Map.Entry<BigraphEntity.Link, LinkedHashSet<BigraphEntity.Port>>> entriesRewrite = linkPortMapRewrite.entrySet();
        for (Map.Entry<BigraphEntity.Link, LinkedHashSet<BigraphEntity.Port>> each : entriesRewrite) {
            LinkedHashSet<BigraphEntity.Port> ports = each.getValue();
            BigraphEntity.Link link = each.getKey();
            for (BigraphEntity.Port eachPort : ports) {
                BigraphEntity.NodeEntity<DefaultDynamicControl> nodeOfPort = reactum.getNodeOfPort(eachPort);
                String portVarName;
                if (newNodesAdded.contains(nodeOfPort)) { // A new node might be added in the reactum
                    portVarName = nodeOfPort.getName() + "_p" + eachPort.getIndex();
                } else {
                    if (trackingMap.get(nodeOfPort.getName()) != null && !trackingMap.get(nodeOfPort.getName()).isEmpty()) {
                        portVarName = trackingMap.get(nodeOfPort.getName()) + "_p" + eachPort.getIndex();
                    } else {
                        // Throw an exception that trackingMap entry is missing which must be provided if the current node is not a newly created one in the rule's reactum
                        throw new RuntimeException("There was no mapping in the tracking map specified for the node " + nodeOfPort.getName() + ", and it was not newly created in the reactum of the rule.");
                    }
                }
                String newLinkName = Optional.of(trackingMap.get(link.getName())).orElse(link.getName());
                String portLinkRel = createGraphlet(portVarName, createVarTypeDecl("", REFERENCE_LINK), newLinkName); //
                sb.append(portLinkRel);
                sb.append(LINE_SEP);
            }
        }

        //Idle outer names in the reactum
        reactum.getOuterNames().stream().filter(x -> reactum.getPointsFromLink(x).isEmpty()).forEach(idleLink -> {
            String newLinkName = Optional.of(trackingMap.get(idleLink.getName())).orElse(idleLink.getName());
            sb.append(createVarTypeDeclWithComma(newLinkName, CLASS_OUTERNAME)).append(LINE_SEP);
        });


        // Now append the generic code template that handle the copy of substructures determined by the sites
        // Create the necessary map to build the indexMap
        redex.getSites().forEach(s -> {
            BigraphEntity<?> parent = redex.getParent(s);
            if (BigraphEntityType.isNode(parent)) {
                redexSiteToReactumSite.put(((BigraphEntity.NodeEntity) parent).getName(), null);
            }
        });
        reactum.getSites().forEach(s -> {
            BigraphEntity<?> parent = reactum.getParent(s);
            if (BigraphEntityType.isNode(parent)) {
                String trueName = trackingMap.getOrDefault(((BigraphEntity.NodeEntity) parent).getName(), ((BigraphEntity.NodeEntity) parent).getName());
                redexSiteToReactumSite.keySet()
                        .forEach(nodeId -> {
                            Optional<BigraphEntity.NodeEntity<DefaultDynamicControl>> nodeById = getNodeById(nodeId, redex);
                            if (nodeById.isPresent()) {
                                Optional<BigraphEntity.SiteEntity> first = redex.getChildrenOf(nodeById.get()).stream().filter(BigraphEntityType::isSite)
                                        .map(t -> (BigraphEntity.SiteEntity) t)
                                        .filter(t -> {
                                            return t.getIndex() == s.getIndex();
                                        }).findFirst();
                                if (first.isPresent()) {
                                    redexSiteToReactumSite.put(nodeId, trueName);
                                }
                            }
                        });
            }
        });
        sb.append(LINE_SEP);
        sb.append(yieldPart());
        sb.append(LINE_SEP);
        sb.append(createMap_allMatchedNodes(varMap.keySet()));
        sb.append(LINE_SEP);
        sb.append(createMap_indexMap(redexSiteToReactumSite));
        sb.append(LINE_SEP);
        sb.append(createMap_tasks());
        sb.append(LINE_SEP);
        sb.append(openEvalBlock());
        sb.append(LINE_SEP);
        sb.append(TEMPLATE_COPY_SITE_CONTENTS);
        sb.append(LINE_SEP);
        sb.append(closeBlock());

        // We are finished now
        sb.append(closeBlock()); //replace block of the rule
        sb.append(closeRule()); // close the rule
        sb.append(LINE_SEP);
        return sb.toString();
    }

    protected String openRule(String ruleName) {
        return String.format("rule %s {%s", ruleName, LINE_SEP);
    }

    protected String openEvalBlock() {
        return String.format("eval {%s", LINE_SEP);
    }

    protected String closeRule() {
        return LINE_SEP + "}";
    }

    protected String yieldPart() {
        return "---" + LINE_SEP;
    }

    private String createMap_allMatchedNodes(Set<String> nodeVariableNames) {
        if (nodeVariableNames == null || nodeVariableNames.size() == 0) {
            return "def ref allMatchedNodes:set<Node> = set<Node>{};";
        }
        return String.format("def ref allMatchedNodes:set<Node> = set<Node>{%s};",
                String.join(", ", nodeVariableNames));
    }

    private String createMap_nodesWithSites(Set<String> nodeVariableNames) {
        if (nodeVariableNames == null || nodeVariableNames.size() == 0) {
            return "def ref nodesWithSites:set<Node> = set<Node>{};";
        }
        return String.format("def ref nodesWithSites:set<Node> = set<Node>{%s};",
                String.join(", ", nodeVariableNames));
    }

    private String createMap_tasks() {
        return "def ref tasks:map<Edge,Node> = map<Edge,Node>{};";
    }

    private String createMap_indexMap(Map<String, String> redexSiteToReactumSite) {
        if (redexSiteToReactumSite == null || redexSiteToReactumSite.size() == 0) {
            return "def ref indexMap:map<Node,Node> = map<Node,Node>{};";
        }
        StringBuilder sb = new StringBuilder();
        redexSiteToReactumSite.entrySet().stream().forEach(entry -> {
            String value = entry.getValue() == null ? "null" : entry.getValue();
            sb.append(entry.getKey()).append(" -> ").append(value).append(", ").append(LINE_SEP);
        });
        sb.replace(sb.lastIndexOf(","), sb.lastIndexOf(",") + 1, "");
        return String.format("def ref indexMap:map<Node,Node> = map<Node,Node>{%s%s};", LINE_SEP, sb);
    }

    protected Optional<BigraphEntity.NodeEntity<DefaultDynamicControl>> getNodeById(String nodeId, PureBigraph bigraph) {
        return bigraph.getNodes().stream().filter(x -> x.getName().equals(nodeId)).findFirst();
    }

    /**
     * specific for bigraph rules
     *
     * @param node            the node
     * @param bigraph         the bigraph containing the node
     * @param withTrackingMap resolve node identifier with the help of the tracking map or not (for the reactum only)
     * @return the identifier of the parent
     */
    protected String getParentId(BigraphEntity.NodeEntity<DefaultDynamicControl> node, PureBigraph bigraph, boolean withTrackingMap) {
        BigraphEntity<?> parent = bigraph.getParent(node);
        String lbl;
        if (BigraphEntityType.isRoot(parent)) {
            lbl = "r0";
        } else {
            assert BigraphEntityType.isNode(parent);
            lbl = ((BigraphEntity.NodeEntity) parent).getName();
            if (trackingMap.containsKey(lbl) && withTrackingMap) {
                lbl = trackingMap.get(lbl);
            }
//            String parentId = ((BigraphEntity.NodeEntity) parent).getName();
//            if (trackingMap.containsKey(parentId) && withTrackingMap) {
//                lbl = trackingMap.get(parentId);
//            } else {
//                lbl = parentId;
//            }
        }
        return lbl;
    }

}
