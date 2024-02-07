package org.bigraphs.grgen.converter.demo;

import org.bigraphs.framework.core.ControlStatus;
import org.bigraphs.framework.core.datatypes.FiniteOrdinal;
import org.bigraphs.framework.core.datatypes.StringTypedName;
import org.bigraphs.framework.core.impl.signature.DefaultDynamicSignature;
import org.bigraphs.framework.core.impl.signature.DynamicSignatureBuilder;

import static org.bigraphs.framework.core.factory.BigraphFactory.pureSignatureBuilder;

public class DemoSignatureProvider {

    private static final DemoSignatureProvider instance = new DemoSignatureProvider();

    private DemoSignatureProvider() {}

    public static DemoSignatureProvider getInstance() {
        return instance;
    }

    public synchronized DefaultDynamicSignature petriNet() {
        DefaultDynamicSignature signature = pureSignatureBuilder()
                .addControl("Place", 1)
//                .addControl("Place2", 1)
                .addControl("Transition", 2)
                .addControl("Token", 0)
                .create();
        return signature;
    }

    public DefaultDynamicSignature smartHome() {
        DynamicSignatureBuilder signatureBuilder = pureSignatureBuilder();
        DefaultDynamicSignature signature = signatureBuilder
                .addControl("Building", 0)
                .addControl("Room", 0)
                .newControl().identifier("User").arity(1).status(ControlStatus.ATOMIC).assign()
                .newControl(StringTypedName.of("Computer"), FiniteOrdinal.ofInteger(1)).status(ControlStatus.ATOMIC).assign().create();
        return signature;
    }

}
