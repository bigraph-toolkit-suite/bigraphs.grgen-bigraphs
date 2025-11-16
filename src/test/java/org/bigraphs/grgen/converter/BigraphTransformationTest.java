package org.bigraphs.grgen.converter;

import org.bigraphs.framework.core.BigraphFileModelManagement;
import org.bigraphs.framework.core.exceptions.InvalidConnectionException;
import org.bigraphs.framework.core.exceptions.builder.TypeNotExistsException;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.signature.DynamicSignature;
import org.bigraphs.grgen.converter.demo.DemoBigraphProvider;
import org.bigraphs.grgen.converter.demo.DemoSignatureProvider;
import org.bigraphs.grgen.converter.impl.PureBigraphTransformer;
import org.bigraphs.testing.BigraphUnitTestSupport;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * This class tests the "host graph" transformation capabilities.
 * The host graph is a model instance conforming to its metamodel.
 * (The signature represents the graph metamodel of GrGen.NET.)
 *
 * @author Dominik Grzelak
 */
public class BigraphTransformationTest implements BigraphUnitTestSupport {
    public static final String TARGET_DUMP_PATH = "src/test/resources/dump/smarthome/";

    @Test
    public void testPetriNetBigraphToGrGenGraphMetaModel() throws Exception {
        DemoSignatureProvider signatureProvider = DemoSignatureProvider.getInstance();
        DynamicSignature sig = signatureProvider.petriNet();
        DemoBigraphProvider bigraphProvider = DemoBigraphProvider.getInstance();
        PureBigraph bigraph = bigraphProvider.petriNet(sig);
//        PureBigraph bigraph = bigraphProvider.petriNetOpenLinks(sig);
        BigraphFileModelManagement.Store.exportAsInstanceModel(bigraph, System.out);
        BigraphFileModelManagement.Store.exportAsMetaModel(bigraph, System.out);

        PureBigraphTransformer transformer = new PureBigraphTransformer()
                .withOppositeEdges(false);
        String grgenGraphModel = transformer.toString(bigraph);
        System.out.println(grgenGraphModel);
    }

    @Test
    public void testSmartHome() throws InvalidConnectionException, TypeNotExistsException, IOException {
        PureBigraph pureBigraph = DemoBigraphProvider.getInstance().smartHome(DemoSignatureProvider.getInstance().smartHome());
        toPNG(pureBigraph, "agent", TARGET_DUMP_PATH, true);

        PureBigraphTransformer transformer = new PureBigraphTransformer()
                .withOppositeEdges(false);
        String grgenGraphModel = transformer.toString(pureBigraph);
        System.out.println(grgenGraphModel);
    }
}
