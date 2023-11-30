package org.example;

import org.bigraphs.framework.core.BigraphFileModelManagement;
import org.bigraphs.framework.core.exceptions.InvalidConnectionException;
import org.bigraphs.framework.core.exceptions.builder.TypeNotExistsException;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.signature.DefaultDynamicSignature;
import org.bigraphs.framework.visualization.BigraphGraphvizExporter;
import org.example.demo.DemoBigraphProvider;
import org.example.demo.DemoSignatureProvider;
import org.example.impl.PureBigraphTransformer;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

public class BigraphTransformationTest implements BigraphUnitTestSupport {
    public static final String TARGET_DUMP_PATH = "src/test/resources/dump/smarthome/";

    @Test
    public void testPetriNetBigraphToGrGenGraphMetaModel() throws Exception {
        DemoSignatureProvider signatureProvider = DemoSignatureProvider.getInstance();
        DefaultDynamicSignature sig = signatureProvider.petriNet();
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
        eb(pureBigraph, "agent", TARGET_DUMP_PATH, true);

        PureBigraphTransformer transformer = new PureBigraphTransformer()
                .withOppositeEdges(false);
        String grgenGraphModel = transformer.toString(pureBigraph);
        System.out.println(grgenGraphModel);
    }
}
