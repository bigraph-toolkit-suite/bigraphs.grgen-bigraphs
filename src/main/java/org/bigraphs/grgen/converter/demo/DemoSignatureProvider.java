package org.bigraphs.grgen.converter.demo;

import org.bigraphs.framework.core.ControlStatus;
import org.bigraphs.framework.core.datatypes.FiniteOrdinal;
import org.bigraphs.framework.core.datatypes.StringTypedName;
import org.bigraphs.framework.core.impl.signature.DynamicSignature;
import org.bigraphs.framework.core.impl.signature.DynamicSignatureBuilder;

import static org.bigraphs.framework.core.factory.BigraphFactory.pureSignatureBuilder;

public class DemoSignatureProvider {

    private static final DemoSignatureProvider instance = new DemoSignatureProvider();

    private DemoSignatureProvider() {}

    public static DemoSignatureProvider getInstance() {
        return instance;
    }

    public synchronized DynamicSignature petriNet() {
        DynamicSignature signature = pureSignatureBuilder()
                .add("Place", 1)
//                .add("Place2", 1)
                .add("Transition", 2)
                .add("Token", 0)
                .create();
        return signature;
    }

    public DynamicSignature smartHome() {
        DynamicSignatureBuilder signatureBuilder = pureSignatureBuilder();
        DynamicSignature signature = signatureBuilder
                .add("Building", 0)
                .add("Room", 0)
                .newControl().identifier("User").arity(1).status(ControlStatus.ATOMIC).assign()
                .newControl(StringTypedName.of("Computer"), FiniteOrdinal.ofInteger(1)).status(ControlStatus.ATOMIC).assign().create();
        return signature;
    }

}
