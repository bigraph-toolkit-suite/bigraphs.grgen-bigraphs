package org.example.demo;

import org.bigraphs.framework.core.exceptions.InvalidConnectionException;
import org.bigraphs.framework.core.exceptions.builder.TypeNotExistsException;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.pure.PureBigraphBuilder;
import org.bigraphs.framework.core.impl.signature.DefaultDynamicSignature;

import static org.bigraphs.framework.core.factory.BigraphFactory.pureBuilder;

public class DemoBigraphProvider {

    private static final DemoBigraphProvider instance = new DemoBigraphProvider();

    private DemoBigraphProvider() {
    }

    public static DemoBigraphProvider getInstance() {
        return instance;
    }

    public synchronized PureBigraph petriNet(DefaultDynamicSignature signature) throws InvalidConnectionException, TypeNotExistsException {
        PureBigraphBuilder<DefaultDynamicSignature> builder = pureBuilder(signature);

//        builder.createOuterName("abcd");
        builder.createRoot()
                .addChild("Place").linkToInner("tmp").down().addChild("Token").addChild("Token").up()
                .addChild("Transition").linkToInner("tmp").linkToInner("tmp2")
                .addChild("Place").linkToInner("tmp2")
        ;
        builder.closeAllInnerNames();

        return builder.createBigraph();
    }

    public synchronized PureBigraph petriNet_withOuterNames(DefaultDynamicSignature signature) throws InvalidConnectionException, TypeNotExistsException {
        PureBigraphBuilder<DefaultDynamicSignature> builder = pureBuilder(signature);

        builder.createOuterName("idleOuter");
        builder.createRoot()
                .addChild("Place").linkToOuter("tmp").down().addChild("Token").addChild("Token").up()
                .addChild("Transition").linkToOuter("tmp").linkToOuter("tmp2")
                .addChild("Place").linkToOuter("tmp2")
        ;

        return builder.createBigraph();
    }

    public synchronized PureBigraph smartHome(DefaultDynamicSignature signature) throws InvalidConnectionException, TypeNotExistsException {
        PureBigraphBuilder<DefaultDynamicSignature> builder = pureBuilder(signature);

        builder.createRoot()
                .addChild("Building").down()
                /**/.addChild("Room").down().addChild("User", "Alice").addChild("Computer", "Alice").up()
                /**/.addChild("Room").down().addChild("User", "Bob").up()
        ;

        return builder.createBigraph();
    }

}
