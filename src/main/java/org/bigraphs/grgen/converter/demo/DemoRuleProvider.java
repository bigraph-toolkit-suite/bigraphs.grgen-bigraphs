package org.bigraphs.grgen.converter.demo;

import org.bigraphs.framework.core.exceptions.InvalidConnectionException;
import org.bigraphs.framework.core.exceptions.InvalidReactionRuleException;
import org.bigraphs.framework.core.exceptions.builder.TypeNotExistsException;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.pure.PureBigraphBuilder;
import org.bigraphs.framework.core.impl.signature.DynamicSignature;
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

    public synchronized ReactionRule<PureBigraph> petriNetFireRule(DynamicSignature signature) throws InvalidConnectionException, TypeNotExistsException, InvalidReactionRuleException {
        PureBigraphBuilder<DynamicSignature> b1 = pureBuilder(signature);
        PureBigraphBuilder<DynamicSignature> b2 = pureBuilder(signature);

        b1.root()
                .child("Place").linkInner("tmp").down().child("Token").site().top()
                .child("Transition").linkInner("tmp").linkInner("tmp2")
                .child("Place").linkInner("tmp2").down().site().top()
        ;
        b1.closeInner();

        b2.root()
                .child("Place").linkInner("tmp").down().site().top()
                .child("Transition").linkInner("tmp").linkInner("tmp2")
                .child("Place").linkInner("tmp2").down().site().child("Token").top()
        ;
        b2.closeInner();

        return new ParametricReactionRule<>(b1.create(), b2.create()).withLabel("petriNetFireRule");
    }

    public synchronized ReactionRule<PureBigraph> petriNetFireRule_withOuterNames(DynamicSignature signature) throws InvalidConnectionException, TypeNotExistsException, InvalidReactionRuleException {
        PureBigraphBuilder<DynamicSignature> b1 = pureBuilder(signature);
        PureBigraphBuilder<DynamicSignature> b2 = pureBuilder(signature);

        b1.root()
                .child("Place", "tmp").down().child("Token").site().top()
                .child("Transition", "tmp").linkOuter("tmp2")
                .child("Place", "tmp2").down().site().top()
        ;
        b1.closeInner();

        b2.root()
                .child("Place", "tmp").down().site().top()
                .child("Transition", "tmp").linkOuter("tmp2")
                .child("Place", "tmp2").down().site().child("Token").top()
        ;
        b2.closeInner();

        return new ParametricReactionRule<>(b1.create(), b2.create()).withLabel("petriNetFireRule_withOuterNames");
    }

    public synchronized ReactionRule<PureBigraph> petriNetAddRule(DynamicSignature signature) throws InvalidConnectionException, InvalidReactionRuleException {
        PureBigraphBuilder<DynamicSignature> b1 = pureBuilder(signature);
        PureBigraphBuilder<DynamicSignature> b2 = pureBuilder(signature);

        b1.root()
                .child("Place", "x").down().site().top()
        ;

        b2.root()
                .child("Place", "x").down().site().child("Token").top()
        ;

        return new ParametricReactionRule<>(b1.create(), b2.create()).withLabel("petriNetAddTokenRule");
    }


    public ReactionRule<PureBigraph> smartHomeMoveRule(DynamicSignature signature) throws Exception {
        PureBigraphBuilder<DynamicSignature> b1 = pureBuilder(signature);
        PureBigraphBuilder<DynamicSignature> b2 = pureBuilder(signature);

        b1.root()
                .child("Room").down()
                .child("User", "x")
                .site();
        b1.root()
                .child("Room").down()
                .site();

        b2.root()
                .child("Room").down()
                .site();
        b2.root()
                .child("Room").down()
                .child("User", "x")
                .site();

        return new ParametricReactionRule<>(b1.create(), b2.create()).withLabel("smartHomeMoveRule");
    }
}
