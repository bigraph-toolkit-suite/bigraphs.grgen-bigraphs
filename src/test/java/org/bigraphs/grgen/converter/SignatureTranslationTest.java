package org.bigraphs.grgen.converter;

import org.bigraphs.framework.core.BigraphFileModelManagement;
import org.bigraphs.framework.core.impl.signature.DynamicSignature;
import org.bigraphs.grgen.converter.demo.DemoSignatureProvider;
import org.bigraphs.grgen.converter.impl.DynamicSignatureTransformer;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * This class tests the signature transformation capabilities.
 * The signature represents the graph metamodel of GrGen.NET.
 *
 * @author Dominik Grzelak
 */
public class SignatureTranslationTest {

    @Test
    public void testPetriNetSignatureToGrGenGraphMetaModel() throws IOException {
        DemoSignatureProvider signatureProvider = DemoSignatureProvider.getInstance();

        DynamicSignature sig = signatureProvider.petriNet();
        BigraphFileModelManagement.Store.exportAsMetaModel(sig, System.out);
        BigraphFileModelManagement.Store.exportAsInstanceModel(sig, System.out);
        DynamicSignatureTransformer signatureTransformer = new DynamicSignatureTransformer();

        String string = signatureTransformer.toString(sig);

        System.out.println(string);
    }

    @Test
    public void test_smartHome() {
        DynamicSignature sig = DemoSignatureProvider.getInstance().smartHome();
        DynamicSignatureTransformer signatureTransformer = new DynamicSignatureTransformer();
        String string = signatureTransformer.toString(sig);
        System.out.println(string);
    }
}
