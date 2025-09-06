package org.bigraphs.grgen.converter.demo;

import org.bigraphs.framework.core.exceptions.InvalidConnectionException;
import org.bigraphs.framework.core.exceptions.builder.TypeNotExistsException;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.pure.PureBigraphBuilder;
import org.bigraphs.framework.core.impl.signature.DynamicSignature;

import static org.bigraphs.framework.core.factory.BigraphFactory.pureBuilder;

public class DemoBigraphProvider {

    private static final DemoBigraphProvider instance = new DemoBigraphProvider();

    private DemoBigraphProvider() {
    }

    public static DemoBigraphProvider getInstance() {
        return instance;
    }

    public synchronized PureBigraph petriNet(DynamicSignature signature) throws InvalidConnectionException, TypeNotExistsException {
        PureBigraphBuilder<DynamicSignature> builder = pureBuilder(signature);

//        builder.createOuterName("abcd");
        builder.root()
                .child("Place").linkInner("tmp").down().child("Token").child("Token").up()
                .child("Transition").linkInner("tmp").linkInner("tmp2")
                .child("Place").linkInner("tmp2")
        ;
        builder.closeInner();

        return builder.create();
    }

    public synchronized PureBigraph petriNet_withOuterNames(DynamicSignature signature) throws InvalidConnectionException, TypeNotExistsException {
        PureBigraphBuilder<DynamicSignature> builder = pureBuilder(signature);

        builder.createOuter("idleOuter");
        builder.root()
                .child("Place").linkOuter("tmp").down().child("Token").child("Token").up()
                .child("Transition").linkOuter("tmp").linkOuter("tmp2")
                .child("Place").linkOuter("tmp2")
        ;

        return builder.create();
    }

    public synchronized PureBigraph smartHome(DynamicSignature signature) throws InvalidConnectionException, TypeNotExistsException {
        PureBigraphBuilder<DynamicSignature> builder = pureBuilder(signature);

        builder.root()
                .child("Building").down()
                /**/.child("Room").down().child("User", "Alice").child("Computer", "Alice").up()
                /**/.child("Room").down().child("User", "Bob").up()
        ;

        return builder.create();
    }

}
