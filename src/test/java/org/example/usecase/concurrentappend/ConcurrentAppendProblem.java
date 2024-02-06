package org.example.usecase.concurrentappend;

import it.uniud.mads.jlibbig.core.std.Bigraph;
import org.apache.commons.io.FileUtils;
import org.bigraphs.framework.converter.jlibbig.JLibBigBigraphDecoder;
import org.bigraphs.framework.converter.jlibbig.JLibBigBigraphEncoder;
import org.bigraphs.framework.core.BigraphFileModelManagement;
import org.bigraphs.framework.core.datatypes.FiniteOrdinal;
import org.bigraphs.framework.core.datatypes.StringTypedName;
import org.bigraphs.framework.core.impl.BigraphEntity;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.pure.PureBigraphBuilder;
import org.bigraphs.framework.core.impl.signature.DefaultDynamicSignature;
import org.bigraphs.framework.core.impl.signature.DynamicSignatureBuilder;
import org.bigraphs.framework.core.reactivesystem.ParametricReactionRule;
import org.bigraphs.framework.core.reactivesystem.ReactionRule;
import org.bigraphs.framework.simulation.matching.pure.PureReactiveSystem;
import org.bigraphs.framework.simulation.modelchecking.BigraphModelChecker;
import org.bigraphs.framework.simulation.modelchecking.ModelCheckingOptions;
import org.bigraphs.framework.simulation.modelchecking.PureBigraphModelChecker;
import org.example.BigraphUnitTestSupport;
import org.example.RuleTransformer;
import org.bigraphs.framework.core.reactivesystem.TrackingMap;
import org.example.impl.DynamicSignatureTransformer;
import org.example.impl.PureBigraphTransformer;
import org.example.impl.PureParametrizedRuleTransformer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.bigraphs.framework.core.factory.BigraphFactory.pureBuilder;
import static org.bigraphs.framework.core.factory.BigraphFactory.pureSignatureBuilder;
import static org.bigraphs.framework.simulation.modelchecking.ModelCheckingOptions.transitionOpts;

/**
 * The concurrent append problem from the GROOVE paper [ReSV04] in bigraphs.
 *
 * @author Dominik Grzelak
 * @see "[ReSV04] Rensink, Arend; Schmidt, Ákos; Varró, Dániel: Model Checking Graph Transformations: A Comparison of Two Approaches. In: Ehrig, H. ; Engels, G. ; Parisi-Presicce, F. ; Rozenberg, G. (Hrsg.): Graph Transformations, Lecture Notes in Computer Science. Berlin, Heidelberg : Springer, 2004 — ISBN 978-3-540-30203-2, S. 226–241"
 */
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(value = 2) // , jvmArgs = {"-Xms2G", "-Xmx2G"}
@Warmup(iterations = 50, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 30, time = 5, timeUnit = TimeUnit.SECONDS)
public class ConcurrentAppendProblem implements BigraphUnitTestSupport {
    private final static String TARGET_DUMP_PATH = "src/test/resources/dump/append/";
    private final static String TARGET_SAMPLE_PATH = "sample/concurrent-append/";

    private final static boolean AUTO_CLEAN_BEFORE = true;
    private final static boolean EXPORT = true;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ConcurrentAppendProblem.class.getSimpleName())
                .forks(1)
                .build();
        new Runner(opt).run();
    }

    @BeforeAll
    @Setup
    public static void setUp() throws IOException {
        if (AUTO_CLEAN_BEFORE) {
            File dump = new File(TARGET_DUMP_PATH);
            dump.mkdirs();
            FileUtils.cleanDirectory(new File(TARGET_DUMP_PATH));
            new File(TARGET_DUMP_PATH + "states/").mkdir();
        }
    }

    @Test
    void test_GrGen_Encoding() throws Exception {
        // Instantiate transformers
        DynamicSignatureTransformer signatureTransformer = new DynamicSignatureTransformer();

        // Instantiate bigraph models
        DefaultDynamicSignature sig = createSignature();
        BigraphFileModelManagement.Store.exportAsInstanceModel(sig, new FileOutputStream(TARGET_SAMPLE_PATH + "sig.xmi"), "signatureMetaModel.ecore");
        BigraphFileModelManagement.Store.exportAsMetaModel(sig, new FileOutputStream(TARGET_SAMPLE_PATH + "signatureMetaModel.ecore"));
        PureBigraph agent = createAgent(); //specify the number of processes here
        BigraphFileModelManagement.Store.exportAsInstanceModel(agent, new FileOutputStream(TARGET_SAMPLE_PATH + "host.xmi"), "bigraphMetaModel.ecore");
        BigraphFileModelManagement.Store.exportAsMetaModel(agent, new FileOutputStream(TARGET_SAMPLE_PATH + "bigraphMetaModel.ecore"));

        // Translate everything
        // Graph metamodel
        String sigStr = signatureTransformer.toString(sig);
        System.out.println(sigStr);

        //Graph model
        PureBigraphTransformer transformer = new PureBigraphTransformer().withOppositeEdges(false);
        String grgenGraphModel = transformer.toString(agent);
        System.out.println(grgenGraphModel);

        // Rules
        PureParametrizedRuleTransformer t = new PureParametrizedRuleTransformer();
        // Append Rule
        TrackingMap trackAppend = RuleTransformer.createMap();
        trackAppend.put("v0", "v0"); // same
        trackAppend.put("v1", "v1");
        trackAppend.put("v2", "v3");
        trackAppend.put("v3", ""); // new
        trackAppend.put("v4", "");
        trackAppend.put("v5", "");
        trackAppend.put("v6", "v5");
        trackAppend.put("v7", "");
        t.withMap(trackAppend);
        String appendRREnc = t.toString(appendRR());
        System.out.println(appendRREnc);
        BigraphFileModelManagement.Store.exportAsInstanceModel(appendRR().getRedex(), new FileOutputStream(TARGET_SAMPLE_PATH + "appendRule-lhs.xmi"), "bigraphMetaModel.ecore");
        BigraphFileModelManagement.Store.exportAsInstanceModel(appendRR().getReactum(), new FileOutputStream(TARGET_SAMPLE_PATH + "appendRule-rhs.xmi"), "bigraphMetaModel.ecore");


        PureParametrizedRuleTransformer t2 = new PureParametrizedRuleTransformer();
        TrackingMap trackNext = RuleTransformer.createMap();
        trackNext.put("v0", "v0");
        trackNext.put("v1", "v1");
        trackNext.put("v2", "v2");
        trackNext.put("v3", "v3");
        trackNext.put("v4", "v4");
        trackNext.put("v5", "");
        trackNext.put("v6", "");
        trackNext.put("v7", "v5");
        trackNext.put("v8", "v6");
        trackNext.put("e0", "e0");
        trackNext.put("e1", "");
        trackNext.addLinkNames("e0", "e1");
        t2.withMap(trackNext);
        String nextRREnc = t2.toString(nextRR());
        System.out.println(nextRREnc);
        BigraphFileModelManagement.Store.exportAsInstanceModel(nextRR().getRedex(), new FileOutputStream(TARGET_SAMPLE_PATH + "nextRule-lhs.xmi"), "bigraphMetaModel.ecore");
        BigraphFileModelManagement.Store.exportAsInstanceModel(nextRR().getReactum(), new FileOutputStream(TARGET_SAMPLE_PATH + "nextRule-rhs.xmi"), "bigraphMetaModel.ecore");


        PureParametrizedRuleTransformer t3 = new PureParametrizedRuleTransformer();
        TrackingMap trackReturn = RuleTransformer.createMap();
        trackReturn.put("v0", "");
        t3.withMap(trackReturn);
        String returnRREnc = t3.toString(returnRR());
        System.out.println(returnRREnc);
        BigraphFileModelManagement.Store.exportAsInstanceModel(returnRR().getRedex(), new FileOutputStream(TARGET_SAMPLE_PATH + "returnRule-lhs.xmi"), "bigraphMetaModel.ecore");
        BigraphFileModelManagement.Store.exportAsInstanceModel(returnRR().getReactum(), new FileOutputStream(TARGET_SAMPLE_PATH + "returnRule-rhs.xmi"), "bigraphMetaModel.ecore");
    }

    @Test
    @Benchmark
//    @Fork(value = 2, warmups = 50)
//    @BenchmarkMode(Mode.Throughput)
    public void simulate() throws Exception {
        PureBigraph agent = createAgent(); //specify the number of processes here
        ReactionRule<PureBigraph> nextRR = nextRR();
        ReactionRule<PureBigraph> append = appendRR();
        ReactionRule<PureBigraph> returnRR = returnRR();
        PureReactiveSystem reactiveSystem = new PureReactiveSystem();
        reactiveSystem.setAgent(agent);
        reactiveSystem.addReactionRule(nextRR);
        reactiveSystem.addReactionRule(append);
        reactiveSystem.addReactionRule(returnRR);

        ModelCheckingOptions modOpts = setUpSimOpts();
        PureBigraphModelChecker modelChecker = new PureBigraphModelChecker(
                reactiveSystem,
                BigraphModelChecker.SimulationStrategy.Type.BFS,
                modOpts);
//        modelChecker.setReactiveSystemListener(this);
        long start = System.nanoTime();
        modelChecker.execute();
        long diff = System.nanoTime() - start;
        System.out.println(diff);


        //states=51, transitions=80
        System.out.println("Edges: " + modelChecker.getReactionGraph().getGraph().edgeSet().size());
        System.out.println("Vertices: " + modelChecker.getReactionGraph().getGraph().vertexSet().size());

//        ReactionGraphAnalysis<PureBigraph> analysis = ReactionGraphAnalysis.createInstance();
//        List<ReactionGraphAnalysis.PathList<PureBigraph>> pathsToLeaves = analysis.findAllPathsInGraphToLeaves(modelChecker.getReactionGraph());
//        System.out.println(pathsToLeaves.size());

    }

    private ModelCheckingOptions setUpSimOpts() {
        Path completePath = Paths.get(TARGET_DUMP_PATH, "transition_graph.png");
        ModelCheckingOptions opts = ModelCheckingOptions.create();
        opts
                .and(transitionOpts()
                        .setMaximumTransitions(5000)
                        .setMaximumTime(-1)
                        .allowReducibleClasses(true)
                        .create()
                )
                .doMeasureTime(true)
                .and(ModelCheckingOptions.exportOpts()
                        .setReactionGraphFile(new File(completePath.toUri()))
                        .setPrintCanonicalStateLabel(false)
                        .setOutputStatesFolder(new File(TARGET_DUMP_PATH + "states/"))
                        .create()
                )
        ;
        return opts;
    }

    PureBigraph createAgent() throws Exception {
        PureBigraphBuilder<DefaultDynamicSignature> builder = pureBuilder(createSignature());

        BigraphEntity.InnerName tmpA1 = builder.createInnerName("tmpA1");
        BigraphEntity.InnerName tmpA2 = builder.createInnerName("tmpA2");
//        BigraphEntity.InnerName tmpA3 = builder.createInnerName("tmpA3");

        PureBigraphBuilder<DefaultDynamicSignature>.Hierarchy appendcontrol1 = builder.hierarchy("append");
        appendcontrol1
//                .linkToOuter(caller1)
                .linkToInner(tmpA1).addChild("val").down().addChild("N5").top();

        PureBigraphBuilder<DefaultDynamicSignature>.Hierarchy appendcontrol2 = builder.hierarchy("append");
        appendcontrol2
//                .linkToOuter(caller2)
                .linkToInner(tmpA2).addChild("val").down().addChild("N4").top();

//        PureBigraphBuilder<DefaultDynamicSignature>.Hierarchy appendcontrol3 = builder.hierarchy("append");
//        appendcontrol3
////                .linkToOuter(caller3)
//                .linkToInner(tmpA3).addChild("val").down().addChild("N6").top();

        PureBigraphBuilder<DefaultDynamicSignature>.Hierarchy rootCell = builder.hierarchy("main")
//                .linkToOuter(caller1).linkToOuter(caller2)
                ;
        rootCell
                .addChild("list").down().addChild("Cell")
                .down().addChild("this").down()
                .addChild("thisRef").linkToInner(tmpA1)
                .addChild("thisRef").linkToInner(tmpA2)
//                .addChild("thisRef").linkToInner(tmpA3)
                .up()
                .addChild("val").down().addChild("N1").up()
                .addChild("next").down().addChild("Cell").down().addChild("this")
//                .down().addChild("thisRef").addChild("thisRef").up()
                .addChild("val").down().addChild("N2").up()
                .addChild("next").down().addChild("Cell").down().addChild("this")
//                .down().addChild("thisRef").addChild("thisRef").up()
                .addChild("val").down().addChild("N3").up()
                .top();

        builder.createRoot()
                .addChild(rootCell)
                .addChild(appendcontrol1)
                .addChild(appendcontrol2)
//                .addChild(appendcontrol3)
        ;
        builder.closeAllInnerNames();
        PureBigraph bigraph = builder.createBigraph();
//        BigraphFileModelManagement.exportAsInstanceModel(bigraph, System.out);
        if (EXPORT)
            eb(bigraph, "agent", TARGET_DUMP_PATH);
        return bigraph;
    }

    ReactionRule<PureBigraph> nextRR() throws Exception {
        PureBigraphBuilder<DefaultDynamicSignature> builderRedex = pureBuilder(createSignature());
        PureBigraphBuilder<DefaultDynamicSignature> builderReactum = pureBuilder(createSignature());

        BigraphEntity.InnerName tmp0 = builderRedex.createInnerName("tmp");
//        BigraphEntity.OuterName anyRef = builderRedex.createOuterName("anyRef");
//        BigraphEntity.OuterName openRef = builderRedex.createOuterName("openRef");
        builderRedex.createRoot()
                .addChild("this")
                .down().addSite().addChild("thisRef").linkToInner(tmp0).up()
//                .addSite()
//                .addChild("val").down().addSite().top()
                .addChild("next").down().addChild("Cell").down().addSite().addChild("this").down()
//                .addChild("thisRef")
                .addSite().up()
                .top()
        ;
        //
        builderRedex.createRoot()
//                .addChild("appendcontrol", "caller").linkToInner(tmp0).down()
                .addChild("append").linkToInner(tmp0).down()
                .addChild("val").down().addSite().top()
        ;
        builderRedex.closeAllInnerNames();

//        BigraphEntity.OuterName anyRef2 = builderReactum.createOuterName("anyRef");
//        BigraphEntity.OuterName openRef2 = builderReactum.createOuterName("openRef");
        BigraphEntity.InnerName tmp21 = builderReactum.createInnerName("tmp1");
        BigraphEntity.InnerName tmp22 = builderReactum.createInnerName("tmp2");
        builderReactum.createRoot()
                .addChild("this").down().addSite().addChild("thisRef").linkToInner(tmp22).up()
//                .addChild("val").down().addSite().top()
//                .addSite()
                .addChild("next").down().addChild("Cell").down().addSite()
                .addChild("this").down().addChild("thisRef").linkToInner(tmp21)
                .addSite()
                .top()
        ;
        //
        builderReactum.createRoot()
//                .addChild("append", "caller").linkToInner(tmp22)
//                .down().addChild("appendcontrol", "caller").linkToInner(tmp21)
                .addChild("append").linkToInner(tmp22)
                .down().addChild("append").linkToInner(tmp21)
                .down()
                .addChild("val").down().addSite().up()

        ;
        builderReactum.closeAllInnerNames();

        PureBigraph redex = builderRedex.createBigraph();
        PureBigraph reactum = builderReactum.createBigraph();
        if (EXPORT) {
            BigraphFileModelManagement.Store.exportAsInstanceModel(redex, System.out);
            BigraphFileModelManagement.Store.exportAsInstanceModel(reactum, System.out);
            eb(redex, "cap-next-lhs", TARGET_DUMP_PATH);
            eb(reactum, "cap-next-rhs", TARGET_DUMP_PATH);
        }

//        JLibBigBigraphEncoder encoder = new JLibBigBigraphEncoder();
//        JLibBigBigraphDecoder decoder = new JLibBigBigraphDecoder();
//        Bigraph encodedRedex = encoder.encode(redex);
//        Bigraph encodedReactum = encoder.encode(reactum, encodedRedex.getSignature());
//        RewritingRule rewritingRule = new RewritingRule(encodedRedex, encodedReactum, 0, 1, 2, 3, 4);

        ReactionRule<PureBigraph> rr = new ParametricReactionRule<>(redex, reactum).withLabel("nextRule");
        return rr;
    }

    // create a new cell with the value
    // Only append a new value when last cell is reached, ie, without a next control
    ReactionRule<PureBigraph> appendRR() throws Exception {
        PureBigraphBuilder<DefaultDynamicSignature> builderRedex = pureBuilder(createSignature());
        PureBigraphBuilder<DefaultDynamicSignature> builderReactum = pureBuilder(createSignature());

//        BigraphEntity.OuterName thisRefAny = builderRedex.createOuterName("thisRefAny");
//        BigraphEntity.OuterName thisRefA1 = builderRedex.createOuterName("thisRefA1");
        BigraphEntity.InnerName tmp = builderRedex.createInnerName("tmp");
        builderRedex.createRoot()
                .addChild("Cell")
                .down()
                .addChild("this").down().addChild("thisRef").linkToInner(tmp).addSite().up()
                .addChild("val").down().addSite().top()
        ;
        //
        builderRedex.createRoot()
//                .addChild("appendcontrol", "caller").linkToInner(tmp).down()
                .addChild("append").linkToInner(tmp).down()
                .addChild("val").down().addSite().up()

        ;
        builderRedex.closeAllInnerNames();

//        BigraphEntity.OuterName thisRefRAny = builderReactum.createOuterName("thisRefAny");
//        BigraphEntity.OuterName thisRefRA1 = builderReactum.createOuterName("thisRefA1");
//        BigraphEntity.InnerName tmp1 = builderReactum.createInnerName("tmp");
        builderReactum.createRoot()
                .addChild("Cell")
                .down()
                .addChild("this").down().addSite().up() //.addChild("thisRef")
                .addChild("val").down().addSite().up()
                .addChild("next").down().addChild("Cell").down().addChild("this").addChild("val").down().addSite().top();
        //
        builderReactum.createRoot()
//                .addChild("Void", "caller")
                .addChild("Void")
        ;

        PureBigraph redex = builderRedex.createBigraph();
        PureBigraph reactum = builderReactum.createBigraph();
        if (EXPORT) {
            eb(redex, "cap-append-lhs", TARGET_DUMP_PATH);
            eb(reactum, "cap-append-rhs", TARGET_DUMP_PATH);
        }

//        JLibBigBigraphEncoder encoder = new JLibBigBigraphEncoder();
//        JLibBigBigraphDecoder decoder = new JLibBigBigraphDecoder();
//        Bigraph encodedRedex = encoder.encode(redex);
//        Bigraph encodedReactum = encoder.encode(reactum, encodedRedex.getSignature());
//        RewritingRule rewritingRule = new RewritingRule(encodedRedex, encodedReactum, 0, 1, 2);

        ReactionRule<PureBigraph> rr = new ParametricReactionRule<>(redex, reactum).withLabel("appendRule");
        return rr;
    }

    //if values are the same...
    ReactionRule<PureBigraph> stopRR() throws Exception {
        return null;
    }

    ReactionRule<PureBigraph> returnRR() throws Exception {
        PureBigraphBuilder<DefaultDynamicSignature> builderRedex = pureBuilder(createSignature());
        PureBigraphBuilder<DefaultDynamicSignature> builderReactum = pureBuilder(createSignature());

        BigraphEntity.InnerName tmp1 = builderRedex.createInnerName("tmp");
        builderRedex.createRoot()
                .addChild("thisRef").linkToInner(tmp1)
        ;
        //
        builderRedex.createRoot()
//                .addChild("append", "caller").linkToInner(tmp1).down().addChild("Void", "caller")
                .addChild("append").linkToInner(tmp1).down().addChild("Void")

        ;
        builderRedex.closeAllInnerNames();


        builderReactum.createRoot()
//                .addChild("thisRef")
        ;
        //
        builderReactum.createRoot()
//                .addChild("Void", "caller")
                .addChild("Void")
        ;
        builderReactum.closeAllInnerNames();

        PureBigraph redex = builderRedex.createBigraph();
        PureBigraph reactum = builderReactum.createBigraph();
        if (EXPORT) {
            eb(redex, "cap-return-lhs", TARGET_DUMP_PATH);
            eb(reactum, "cap-return-rhs", TARGET_DUMP_PATH);
        }
        JLibBigBigraphEncoder encoder = new JLibBigBigraphEncoder();
        Bigraph encodedRedex = encoder.encode(redex);
        JLibBigBigraphDecoder decoder = new JLibBigBigraphDecoder();
        PureBigraph decode = decoder.decode(encodedRedex, redex.getSignature());
        if (EXPORT) {
            eb(decode, "return_decoded_1", TARGET_DUMP_PATH);
        }
//        RewritingRule rewritingRule = new RewritingRule(encodedRedex, encodedRedex, 0, 1, 2);
        ReactionRule<PureBigraph> rr = new ParametricReactionRule<>(redex, reactum).withLabel("returnRule");
        return rr;
    }

    private DefaultDynamicSignature createSignature() {
        DynamicSignatureBuilder defaultBuilder = pureSignatureBuilder();
        defaultBuilder
                .addControl("append", 1) // as much as we callers have
                .newControl().identifier(StringTypedName.of("main")).arity(FiniteOrdinal.ofInteger(0)).assign()
                .newControl().identifier(StringTypedName.of("list")).arity(FiniteOrdinal.ofInteger(0)).assign()
                .newControl().identifier(StringTypedName.of("this")).arity(FiniteOrdinal.ofInteger(0)).assign()
                .newControl().identifier(StringTypedName.of("thisRef")).arity(FiniteOrdinal.ofInteger(1)).assign() // as much as we have callers
                .newControl().identifier(StringTypedName.of("Cell")).arity(FiniteOrdinal.ofInteger(0)).assign()
                .newControl().identifier(StringTypedName.of("Void")).arity(FiniteOrdinal.ofInteger(0)).assign()
                .newControl().identifier(StringTypedName.of("val")).arity(FiniteOrdinal.ofInteger(0)).assign()
                .newControl().identifier(StringTypedName.of("N1")).arity(FiniteOrdinal.ofInteger(0)).assign() // parameterized control
                .newControl().identifier(StringTypedName.of("N2")).arity(FiniteOrdinal.ofInteger(0)).assign()
                .newControl().identifier(StringTypedName.of("N3")).arity(FiniteOrdinal.ofInteger(0)).assign()
                .newControl().identifier(StringTypedName.of("N4")).arity(FiniteOrdinal.ofInteger(0)).assign()
                .newControl().identifier(StringTypedName.of("N5")).arity(FiniteOrdinal.ofInteger(0)).assign()
                .newControl().identifier(StringTypedName.of("N6")).arity(FiniteOrdinal.ofInteger(0)).assign()
                .newControl().identifier(StringTypedName.of("next")).arity(FiniteOrdinal.ofInteger(0)).assign()
        ;
        return defaultBuilder.create();
    }
}
