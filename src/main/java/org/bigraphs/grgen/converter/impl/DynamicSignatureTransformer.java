package org.bigraphs.grgen.converter.impl;

import org.bigraphs.framework.core.Signature;
import org.bigraphs.grgen.converter.SignatureTransformer;

import java.util.List;
import java.util.stream.Collectors;

import static org.bigraphs.framework.core.BigraphMetaModelConstants.*;

/**
 * This class transforms an Ecore bigraph metamodel (i.e., a type graph extended by a signature) to GrGen.NET's graph metamodel format.
 * The graph metamodel resembles the concrete syntax of GrGen.NET's Graph Model Language.
 * <p>
 * GrGen.NET's graph metamodel represents essentially a control-compatible extension of the basic type graph of bigraphs (refer to [KeTG16]).
 *
 * @author Dominik Grzelak
 * @see <a href="http://arxiv.org/abs/1612.01638">[KeTG16] Kehrer, Timo ; Tsigkanos, Christos ; Ghezzi, Carlo: An EMOF-Compliant Abstract Syntax for Bigraphs. In: Electronic Proceedings in Theoretical Computer Science Bd. 231 (2016), S. 16–30. — arXiv: 1612.01638</a>
 */
public class DynamicSignatureTransformer extends SignatureTransformer {
    private static final String INDEX_ATTRIBUTE = "  ix: int;";

    /**
     * This transforms an Ecore bigraph metamodel (type graph extended by a signature) to GrGen.NET's graph metamodel format.
     *
     * @param signature the signature to transform
     * @return GrGen.NET's graph metamodel (i.e., a control-compatible extension of the basic type graph of bigraphs)
     */
    @Override
    public String toString(Signature<?> signature) {
        StringBuilder sb = new StringBuilder();
        List<String> controlLabels = signature.getControls().stream().map(x -> x.getNamedType().stringValue()).collect(Collectors.toList());

        // (1) Re-create the abstract syntax bigraph, i.e., the Ecore metamodel of bigraphs (see Kehrer et. al. or Dominik's dissertation)
        // First, all abstract node classes
        sb.append(closeClassDefinition(createAbstractNodeClass(CLASS_PLACE))).append(LINE_SEP)
                .append(closeClassDefinition(createAbstractNodeClass(CLASS_POINT))).append(LINE_SEP)
                .append(closeClassDefinition(createAbstractNodeClass(CLASS_LINK))).append(LINE_SEP)
                .append(closeClassDefinition(createAbstractNodeClass(CLASS_NODE))).append(LINE_SEP);

        // Create rest of the node classes extending the respective abstract ones
        // Place Graph
        sb.append(closeClassDefinitionWithAttributes(extendClass(createNodeClass(CLASS_ROOT), CLASS_PLACE), INDEX_ATTRIBUTE)).append(LINE_SEP);
        sb.append(closeClassDefinitionWithAttributes(extendClass(createNodeClass(CLASS_SITE), CLASS_PLACE), INDEX_ATTRIBUTE)).append(LINE_SEP);
        // Link Graph
        sb.append(closeClassDefinitionWithAttributes(extendClass(createNodeClass(CLASS_PORT), CLASS_POINT), INDEX_ATTRIBUTE)).append(LINE_SEP);
        sb.append(closeClassDefinition(extendClass(createNodeClass(CLASS_INNERNAME), CLASS_POINT))).append(LINE_SEP);
        sb.append(closeClassDefinition(extendClass(createNodeClass(CLASS_EDGE), CLASS_LINK))).append(LINE_SEP);
        sb.append(closeClassDefinition(extendClass(createNodeClass(CLASS_OUTERNAME), CLASS_LINK))).append(LINE_SEP);


        // In the following we choose only one direction of the Opposite Edges (towards multiplicity 1..1)
        // Edge class representing the place graph parent map
        sb.append(closeClassDefinition(createEdgeClass(REFERENCE_PARENT))).append(LINE_SEP); //"the node as a parent node"
        sb.append(closeClassDefinition(createEdgeClass(REFERENCE_CHILD))).append(LINE_SEP); //"the node as a parent node"
        // Edge from a port to node
        sb.append(closeClassDefinition(createEdgeClass(REFERENCE_NODE))).append(LINE_SEP); // "the port belongs to this node"
        sb.append(closeClassDefinition(createEdgeClass(REFERENCE_PORT))).append(LINE_SEP); // "the port belongs to this node"
        // Edge from a point to link
        sb.append(closeClassDefinition(createEdgeClass(REFERENCE_LINK))).append(LINE_SEP); // "the point is connected to this link"
        sb.append(closeClassDefinition(createEdgeClass(REFERENCE_POINT))).append(LINE_SEP); // "the point is connected to this link"

        // Edge class representing the links in the hypergraph
//        sb.append(closeClassDefinition(extendClass(createEdgeClass("Hyperedge"), DEFAULT_ABSTRACT_EDGE))).append(LINE_SEP);
        // For the rules later, some helpful super edge classes when querying in graph matching
//        sb.append(closeClassDefinition(createAbstractEdgeClass(REFERENCE_PLACE_GRAPH))).append(LINE_SEP);

        sb.append(LINE_SEP);

        // (2) Extension of the MetaModel
        controlLabels.forEach(x -> {
            sb.append(closeClassDefinition(extendClass(createNodeClass(x), CLASS_NODE))).append(LINE_SEP);
        });

        return sb.toString();
    }
}
