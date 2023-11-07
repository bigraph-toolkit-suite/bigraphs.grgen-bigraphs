package org.example.demo;

import org.bigraphs.framework.core.exceptions.InvalidConnectionException;
import org.bigraphs.framework.core.exceptions.InvalidReactionRuleException;
import org.bigraphs.framework.core.exceptions.builder.TypeNotExistsException;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.pure.PureBigraphBuilder;
import org.bigraphs.framework.core.impl.signature.DefaultDynamicSignature;
import org.bigraphs.framework.core.reactivesystem.ParametricReactionRule;
import org.bigraphs.framework.core.reactivesystem.ReactionRule;

import static org.bigraphs.framework.core.factory.BigraphFactory.pureBuilder;

public class DemoRuleProvider {
    private static final DemoRuleProvider instance = new DemoRuleProvider();

    private DemoRuleProvider() {
    }

    public static DemoRuleProvider getInstance() {
        return instance;
    }

    public synchronized ReactionRule<PureBigraph> petriNetFireRule(DefaultDynamicSignature signature) throws InvalidConnectionException, TypeNotExistsException, InvalidReactionRuleException {
        PureBigraphBuilder<DefaultDynamicSignature> b1 = pureBuilder(signature);
        PureBigraphBuilder<DefaultDynamicSignature> b2 = pureBuilder(signature);

        b1.createRoot()
                .addChild("Place").linkToInner("tmp").down().addChild("Token").addSite().top()
                .addChild("Transition").linkToInner("tmp").linkToInner("tmp2")
                .addChild("Place").linkToInner("tmp2").down().addSite().top()
        ;
        b1.closeAllInnerNames();

        b2.createRoot()
                .addChild("Place").linkToInner("tmp").down().addSite().top()
                .addChild("Transition").linkToInner("tmp").linkToInner("tmp2")
                .addChild("Place").linkToInner("tmp2").down().addSite().addChild("Token").top()
        ;
        b2.closeAllInnerNames();

        return new ParametricReactionRule<>(b1.createBigraph(), b2.createBigraph()).withLabel("petriNetFireRule");
    }

    public synchronized ReactionRule<PureBigraph> petriNetFireRule_withOuterNames(DefaultDynamicSignature signature) throws InvalidConnectionException, TypeNotExistsException, InvalidReactionRuleException {
        PureBigraphBuilder<DefaultDynamicSignature> b1 = pureBuilder(signature);
        PureBigraphBuilder<DefaultDynamicSignature> b2 = pureBuilder(signature);

        b1.createRoot()
                .addChild("Place", "tmp").down().addChild("Token").addSite().top()
                .addChild("Transition", "tmp").linkToOuter("tmp2")
                .addChild("Place", "tmp2").down().addSite().top()
        ;
        b1.closeAllInnerNames();

        b2.createRoot()
                .addChild("Place", "tmp").down().addSite().top()
                .addChild("Transition", "tmp").linkToOuter("tmp2")
                .addChild("Place", "tmp2").down().addSite().addChild("Token").top()
        ;
        b2.closeAllInnerNames();

        return new ParametricReactionRule<>(b1.createBigraph(), b2.createBigraph()).withLabel("petriNetFireRule_withOuterNames");
    }

    //TODO design a correct parallel rule
    public synchronized ReactionRule<PureBigraph> petriNetParallelRule(DefaultDynamicSignature signature) throws InvalidConnectionException, TypeNotExistsException, InvalidReactionRuleException {
        PureBigraphBuilder<DefaultDynamicSignature> b1 = pureBuilder(signature);
        PureBigraphBuilder<DefaultDynamicSignature> b2 = pureBuilder(signature);

        b1.createRoot()
                .addChild("Place").down().addSite().top()
        ;
        b1.createRoot()
                .addChild("Token").top();


        b2.createRoot()
                .addChild("Place").down().addSite().addChild("Token").top()
        ;
        b2.createRoot()
                .addChild("Token").top();

        return new ParametricReactionRule<>(b1.createBigraph(), b2.createBigraph()).withLabel("petriNetFireRule2");
    }

    public synchronized ReactionRule<PureBigraph> petriNetAddRule(DefaultDynamicSignature signature) throws InvalidConnectionException, InvalidReactionRuleException {
        PureBigraphBuilder<DefaultDynamicSignature> b1 = pureBuilder(signature);
        PureBigraphBuilder<DefaultDynamicSignature> b2 = pureBuilder(signature);

        b1.createRoot()
                .addChild("Place", "x").down().addSite().top()
        ;

        b2.createRoot()
                .addChild("Place", "x").down().addSite().addChild("Token").top()
        ;

        return new ParametricReactionRule<>(b1.createBigraph(), b2.createBigraph()).withLabel("petriNetAddTokenRule");
    }


    public ReactionRule<PureBigraph> smartHomeMoveRule(DefaultDynamicSignature signature) throws Exception {
        PureBigraphBuilder<DefaultDynamicSignature> b1 = pureBuilder(signature);
        PureBigraphBuilder<DefaultDynamicSignature> b2 = pureBuilder(signature);

        b1.createRoot()
                .addChild("Room").down()
                .addChild("User", "x")
                .addSite();
        b1.createRoot()
                .addChild("Room").down()
                .addSite();

        b2.createRoot()
                .addChild("Room").down()
                .addSite();
        b2.createRoot()
                .addChild("Room").down()
                .addChild("User", "x")
                .addSite();

        return new ParametricReactionRule<>(b1.createBigraph(), b2.createBigraph()).withLabel("smartHomeMoveRule");
    }
}
