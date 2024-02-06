package org.example;

import org.bigraphs.framework.core.reactivesystem.ReactionRule;

import java.util.HashMap;
import java.util.UUID;
import org.bigraphs.framework.core.reactivesystem.TrackingMap;
/**
 * @author Dominik Grzelak
 */
public abstract class RuleTransformer extends TransformerSupport implements BaseTransformer<ReactionRule<?>> {

    protected TrackingMap trackingMap = createMap();

    // in case the reactum node id as no "image" (Value is empty) but the redex has a node with the same id
    // then, the reactum node/edge id needs the be temporarily substituted but keeping the reference to trackingMap
    private HashMap<String, String> trackingSubstitutionMap = new HashMap<>();

    boolean printNACPattern = true;

    /**
     * Supply a tracking map for the current rule instance.
     *
     * @param trackingMap a tracking map.
     * @return this instance
     */
    public BaseTransformer<ReactionRule<?>> withMap(TrackingMap trackingMap) {
        this.trackingMap = trackingMap;
        return this;
    }

    public boolean isPrintNACPattern() {
        return printNACPattern;
    }

    /**
     * Determine, whether to generate also the GrGen.NET pattern ({@link #TEMPLATE_NAC_NO_SITE_PATTERN}) for each rule.
     * Useful, when multiple bigraphical rules have to be translated.
     * Then, the pattern ({@link #TEMPLATE_NAC_NO_SITE_PATTERN}) can be omitted for all rules but one.
     *
     * @param printNACPattern true or false, whether to output it or not.
     * @return this instance
     */
    public RuleTransformer printNACPattern(boolean printNACPattern) {
        this.printNACPattern = printNACPattern;
        return this;
    }

    public static TrackingMap createMap() {
        return new TrackingMap();
    }

    @Override
    public abstract String toString(ReactionRule<?> element);

    public String trackIdentityOf(String nodeId) {
        if (trackingMap.containsKey(nodeId)) {
            if (trackingMap.get(nodeId) == null || trackingMap.get(nodeId).isEmpty()) {
                if (!trackingSubstitutionMap.containsKey(nodeId)) {
                    String newNodeId = nodeId + "_" + UUID.randomUUID().toString().split("-")[1]+Math.round(Math.random()*1000);
                    trackingSubstitutionMap.put(nodeId, newNodeId);
                }
                return trackingSubstitutionMap.get(nodeId);
            }
            return trackingMap.get(nodeId);
        }
        throw new RuntimeException("Error");
    }


    /**
     * A GrGen.NET pattern specifying a NAC (for atomic controls, or nodes in the rule not containing a site).
     */
    public final String TEMPLATE_NAC_NO_SITE_PATTERN =
            "pattern nac_NodesWithoutSites(src:BNode, var validChildCount: int) {\n" +
                    "    negative {\n" +
                    "        if { adjacentIncoming(src, bPrnt).size() != validChildCount; }\n" +
                    "    }\n" +
                    "}";

    /**
     * A generic piece of GrGen.NET code used within a rule specification.
     * It is used to handle the copies of graph substructures determined by the bigraphical sites of a rule.
     */
    public final String TEMPLATE_COPY_SITE_CONTENTS = "" +
            "def ref nodesWithSites:set<Node> = indexMap.domain();\n" +
            "for(cur:Node in nodesWithSites) {\n" +
            "    emit(cur, \"->\", indexMap[cur], \" \", countAdjacentIncoming(cur), \"\\n\");\n" +
            "    if(indexMap[cur] == null) {\n" +
            "        emit(\"Remove everything because there is no site mapping for the reactum\", adjacentIncoming(cur), \"\\n\");\n" +
            "        for(x__INTERN:Node in adjacentIncoming(cur)) {\n" +
            "            if(typeof(x__INTERN) != BPort) {\n" +
            "                rem(x__INTERN);\n" +
            "            }\n" +
            "        }\n" +
            "        continue;\n" +
            "    }\n" +
            "    if(cur != indexMap[cur]) {\n" +
            "        emit(\"Site mappings are different! Size of children: \", adjacentIncoming(cur).size(), \"\\n\");\n" +
            "        for(x__INTERN:Node in adjacentIncoming(cur)) {\n" +
            "            if(typeof(x__INTERN) != BPort && !(x__INTERN in allMatchedNodes)) {\n" +
            "                emit(\"\\tchild = \", x__INTERN, \" \", typeof(x__INTERN), \" \", indexMap[cur], \" \", outgoing(x__INTERN), \"\\n\");\n" +
            "                for(y:Edge in outgoing(x__INTERN)) {\n" +
            "                    if(typeof(y) == bPrnt) {\n" +
            "                        /// Get new parent node: indexMap[cur] and redirect edge, is better than adding and removing edge\n" +
            "                        emit(\"\\ty\", y, \" \", typeof(y), \" \", nameof(y), \"\\n\");\n" +
            "                        ///redirectTarget(y, indexMap[cur]);\n" +
            "                        tasks.add(y, indexMap[cur]);\n" +
            "                        ///rem(y);\n" +
            "                    }\n" +
            "                }\n" +
            "                ///add(bPrnt, x__INTERN, indexMap[cur]);\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}\n" +
            "def ref keyset:set<Edge> = tasks.domain();\n" +
            "for(k:Edge in keyset) {\n" +
            "   redirectTarget(k, tasks[k]);\n" +
            "}\n";
}
