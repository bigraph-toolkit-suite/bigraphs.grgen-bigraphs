package org.example;

import org.bigraphs.framework.core.BigraphFileModelManagement;
import org.bigraphs.framework.core.impl.signature.DefaultDynamicSignature;
import org.example.demo.DemoSignatureProvider;
import org.example.impl.DynamicSignatureTransformer;
import org.testng.annotations.Test;

import java.io.IOException;

// 16.2.2 Graph Query by Types MANUAL
// Validation Rule that checks opposite edges of the model etc.
public class SignatureTranslationTest {

    @Test
    public void testPetriNetSignatureToGrGenGraphMetaModel() throws IOException {
        DemoSignatureProvider signatureProvider = DemoSignatureProvider.getInstance();

        DefaultDynamicSignature sig = signatureProvider.petriNet();
        BigraphFileModelManagement.Store.exportAsMetaModel(sig, System.out);
        BigraphFileModelManagement.Store.exportAsInstanceModel(sig, System.out);
        DynamicSignatureTransformer signatureTransformer = new DynamicSignatureTransformer();

        String string = signatureTransformer.toString(sig);

        System.out.println(string);
    }

    @Test
    public void test_smartHome() {
        DefaultDynamicSignature sig = DemoSignatureProvider.getInstance().smartHome();
        DynamicSignatureTransformer signatureTransformer = new DynamicSignatureTransformer();
        String string = signatureTransformer.toString(sig);
        System.out.println(string);
    }
}
