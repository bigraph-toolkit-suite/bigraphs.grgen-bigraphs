package org.bigraphs.grgen.converter.usecase.concurrentappend;

import it.uniud.mads.jlibbig.core.std.Bigraph;
import org.apache.commons.io.FileUtils;
import org.bigraphs.framework.converter.bigrapher.BigrapherTransformator;
import org.bigraphs.framework.converter.jlibbig.JLibBigBigraphDecoder;
import org.bigraphs.framework.converter.jlibbig.JLibBigBigraphEncoder;
import org.bigraphs.framework.core.BigraphFileModelManagement;
import org.bigraphs.framework.core.datatypes.FiniteOrdinal;
import org.bigraphs.framework.core.datatypes.StringTypedName;
import org.bigraphs.framework.core.impl.BigraphEntity;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.pure.PureBigraphBuilder;
import org.bigraphs.framework.core.impl.signature.DynamicSignature;
import org.bigraphs.framework.core.impl.signature.DynamicSignatureBuilder;
import org.bigraphs.framework.core.reactivesystem.ParametricReactionRule;
import org.bigraphs.framework.core.reactivesystem.ReactionRule;
import org.bigraphs.framework.simulation.matching.pure.PureReactiveSystem;
import org.bigraphs.framework.simulation.modelchecking.BigraphModelChecker;
import org.bigraphs.framework.simulation.modelchecking.ModelCheckingOptions;
import org.bigraphs.framework.simulation.modelchecking.PureBigraphModelChecker;
import org.bigraphs.grgen.converter.RuleTransformer;
import org.bigraphs.grgen.converter.impl.DynamicSignatureTransformer;
import org.bigraphs.grgen.converter.impl.PureBigraphTransformer;
import org.bigraphs.grgen.converter.impl.PureParametrizedRuleTransformer;
import org.bigraphs.framework.core.reactivesystem.TrackingMap;
import org.bigraphs.testing.BigraphUnitTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.bigraphs.framework.core.factory.BigraphFactory.pureBuilder;
import static org.bigraphs.framework.core.factory.BigraphFactory.pureSignatureBuilder;
import static org.bigraphs.framework.simulation.modelchecking.ModelCheckingOptions.transitionOpts;

/**
 * The concurrent append problem from the GROOVE paper [ReSV04] in bigraphs is modelled here.
 * It is used to perform also some benchmark.
 * The output is intended for comparison with the results of GrGen.NET.
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
@Disabled
public class ConcurrentAppendProblem implements BigraphUnitTestSupport {
    private final static String TARGET_DUMP_PATH = "src/test/resources/dump/append/";
    private final static String TARGET_SAMPLE_PATH = "sample/concurrent-append-p2/";

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
        DynamicSignature sig = createSignature();
//        BigraphFileModelManagement.Store.exportAsInstanceModel(sig, new FileOutputStream(TARGET_SAMPLE_PATH + "sig.xmi"), "signatureMetaModel.ecore");
//        BigraphFileModelManagement.Store.exportAsMetaModel(sig, new FileOutputStream(TARGET_SAMPLE_PATH + "signatureMetaModel.ecore"));
        PureBigraph agent = createAgent(); //specify the number of processes here
//        BigraphFileModelManagement.Store.exportAsInstanceModel(agent, new FileOutputStream(TARGET_SAMPLE_PATH + "host.xmi"), "bigraphMetaModel.ecore");
//        BigraphFileModelManagement.Store.exportAsMetaModel(agent, new FileOutputStream(TARGET_SAMPLE_PATH + "bigraphMetaModel.ecore"));

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
//        BigraphFileModelManagement.Store.exportAsInstanceModel(appendRR().getRedex(), new FileOutputStream(TARGET_SAMPLE_PATH + "appendRule-lhs.xmi"), "bigraphMetaModel.ecore");
//        BigraphFileModelManagement.Store.exportAsInstanceModel(appendRR().getReactum(), new FileOutputStream(TARGET_SAMPLE_PATH + "appendRule-rhs.xmi"), "bigraphMetaModel.ecore");


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
//        BigraphFileModelManagement.Store.exportAsInstanceModel(nextRR().getRedex(), new FileOutputStream(TARGET_SAMPLE_PATH + "nextRule-lhs.xmi"), "bigraphMetaModel.ecore");
//        BigraphFileModelManagement.Store.exportAsInstanceModel(nextRR().getReactum(), new FileOutputStream(TARGET_SAMPLE_PATH + "nextRule-rhs.xmi"), "bigraphMetaModel.ecore");


        PureParametrizedRuleTransformer t3 = new PureParametrizedRuleTransformer();
        TrackingMap trackReturn = RuleTransformer.createMap();
        trackReturn.put("v0", "v0");
        trackReturn.put("v1", "v3");
        t3.withMap(trackReturn);
        String returnRREnc = t3.toString(returnRR());
        System.out.println(returnRREnc);
//        BigraphFileModelManagement.Store.exportAsInstanceModel(returnRR().getRedex(), new FileOutputStream(TARGET_SAMPLE_PATH + "returnRule-lhs.xmi"), "bigraphMetaModel.ecore");
//        BigraphFileModelManagement.Store.exportAsInstanceModel(returnRR().getReactum(), new FileOutputStream(TARGET_SAMPLE_PATH + "returnRule-rhs.xmi"), "bigraphMetaModel.ecore");
    }

    @Test
    @Benchmark
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

        BigrapherTransformator bigrapherEnc = new BigrapherTransformator();
        System.out.println(bigrapherEnc.toString(reactiveSystem));

        ModelCheckingOptions modOpts = setUpSimOpts();
        PureBigraphModelChecker modelChecker = new PureBigraphModelChecker(
                reactiveSystem,
                BigraphModelChecker.SimulationStrategy.Type.BFS,
                modOpts);
        long start = System.nanoTime();
        modelChecker.execute();
        long diff = System.nanoTime() - start;
        System.out.println(diff);
        System.out.println("Output wrtiten to " + TARGET_DUMP_PATH);

        //states=51, transitions=80
        System.out.println("Edges: " + modelChecker.getReactionGraph().getGraph().edgeSet().size());
        System.out.println("Vertices: " + modelChecker.getReactionGraph().getGraph().vertexSet().size());

//        DOTExporter exporter = new DOTExporter<>();
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
//                        .setFormatsEnabled(List.of(ModelCheckingOptions.ExportOptions.Format.PNG))
                        .setOutputStatesFolder(new File(TARGET_DUMP_PATH + "states/"))
                        .create()
                )
        ;
        return opts;
    }

    PureBigraph createAgent() throws Exception {
        PureBigraphBuilder<DynamicSignature> builder = pureBuilder(createSignature());

        BigraphEntity.InnerName tmpA1 = builder.createInner("tmpA1");
        BigraphEntity.InnerName tmpA2 = builder.createInner("tmpA2");
//        BigraphEntity.InnerName tmpA3 = builder.createInner("tmpA3"); // third process

        PureBigraphBuilder<DynamicSignature>.Hierarchy appendcontrol1 = builder.hierarchy("append");
        appendcontrol1
//                .linkOuter(caller1) // with outer names instead of closed links
                .linkInner(tmpA1).child("val").down().child("N4").top();

        PureBigraphBuilder<DynamicSignature>.Hierarchy appendcontrol2 = builder.hierarchy("append");
        appendcontrol2
//                .linkOuter(caller2) // with outer names instead of closed links
                .linkInner(tmpA2).child("val").down().child("N5").top();

//        PureBigraphBuilder<DynamicSignature>.Hierarchy appendcontrol3 = builder.hierarchy("append"); // third process
//        appendcontrol3
////                .linkOuter(caller3) // with outer names instead of closed links
//                .linkInner(tmpA3).child("val").down().child("N6").top();

        PureBigraphBuilder<DynamicSignature>.Hierarchy rootCell = builder.hierarchy("main")
//                .linkOuter(caller1).linkOuter(caller2) // with outer names instead of closed links
                ;
        rootCell
                .child("list").down().child("Cell")
                .down().child("this").down()
                .child("thisRef").linkInner(tmpA1)
                .child("thisRef").linkInner(tmpA2)
//                .child("thisRef").linkInner(tmpA3) // third process
                .up()
                .child("val").down().child("N1").up()
                .child("next").down().child("Cell").down().child("this")
//                .down().child("thisRef").child("thisRef").up()
                .child("val").down().child("N2").up()
                .child("next").down().child("Cell").down().child("this")
//                .down().child("thisRef").child("thisRef").up()
                .child("val").down().child("N3").up()
                .top();

        builder.root()
                .child(rootCell)
                .child(appendcontrol1)
                .child(appendcontrol2)
//                .child(appendcontrol3) // third process
        ;
        builder.closeInner();
        PureBigraph bigraph = builder.create();
//        BigraphFileModelManagement.exportAsInstanceModel(bigraph, System.out);
        if (EXPORT)
            toPNG(bigraph, "agent", TARGET_DUMP_PATH);
        return bigraph;
    }

    ReactionRule<PureBigraph> nextRR() throws Exception {
        PureBigraphBuilder<DynamicSignature> builderRedex = pureBuilder(createSignature());
        PureBigraphBuilder<DynamicSignature> builderReactum = pureBuilder(createSignature());

        BigraphEntity.InnerName tmp0 = builderRedex.createInner("tmp");
//        BigraphEntity.OuterName anyRef = builderRedex.createOuterName("anyRef");
//        BigraphEntity.OuterName openRef = builderRedex.createOuterName("openRef");
        builderRedex.root()
                .child("this")
                .down().site().child("thisRef").linkInner(tmp0).up()
//                .site()
//                .child("val").down().site().top()
                .child("next").down().child("Cell").down().site().child("this").down()
//                .child("thisRef")
                .site().up()
                .top()
        ;
        //
        builderRedex.root()
                .child("append").linkInner(tmp0).down()
                .child("val").down().site().top()
        ;
        builderRedex.closeInner();

//        BigraphEntity.OuterName anyRef2 = builderReactum.createOuterName("anyRef");
//        BigraphEntity.OuterName openRef2 = builderReactum.createOuterName("openRef");
        BigraphEntity.InnerName tmp21 = builderReactum.createInner("tmp1");
        BigraphEntity.InnerName tmp22 = builderReactum.createInner("tmp2");
        builderReactum.root()
                .child("this").down().site().child("thisRef").linkInner(tmp22).up()
//                .child("val").down().site().top()
//                .site()
                .child("next").down().child("Cell").down().site()
                .child("this").down().child("thisRef").linkInner(tmp21)
                .site()
                .top()
        ;
        //
        builderReactum.root()
                .child("append").linkInner(tmp22)
                .down().child("append").linkInner(tmp21)
                .down()
                .child("val").down().site().up()

        ;
        builderReactum.closeInner();

        PureBigraph redex = builderRedex.create();
        PureBigraph reactum = builderReactum.create();
        if (EXPORT) {
            BigraphFileModelManagement.Store.exportAsInstanceModel(redex, System.out);
            BigraphFileModelManagement.Store.exportAsInstanceModel(reactum, System.out);
            toPNG(redex, "cap-next-lhs", TARGET_DUMP_PATH);
            toPNG(reactum, "cap-next-rhs", TARGET_DUMP_PATH);
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
        PureBigraphBuilder<DynamicSignature> builderRedex = pureBuilder(createSignature());
        PureBigraphBuilder<DynamicSignature> builderReactum = pureBuilder(createSignature());

//        BigraphEntity.OuterName thisRefAny = builderRedex.createOuterName("thisRefAny");
//        BigraphEntity.OuterName thisRefA1 = builderRedex.createOuterName("thisRefA1");
        BigraphEntity.InnerName tmp = builderRedex.createInner("tmp");
        builderRedex.root()
                .child("Cell")
                .down()
                .child("this").down().child("thisRef").linkInner(tmp).site().up()
                .child("val").down().site().top()
        ;
        //
        builderRedex.root()
                .child("append").linkInner(tmp).down()
                .child("val").down().site().up()

        ;
        builderRedex.closeInner();

//        BigraphEntity.OuterName thisRefRAny = builderReactum.createOuterName("thisRefAny");
//        BigraphEntity.OuterName thisRefRA1 = builderReactum.createOuterName("thisRefA1");
//        BigraphEntity.InnerName tmp1 = builderReactum.createInner("tmp");
        builderReactum.root()
                .child("Cell")
                .down()
                .child("this").down().site().up() //.child("thisRef")
                .child("val").down().site().up()
                .child("next").down().child("Cell").down().child("this").child("val").down().site().top();
        //
        builderReactum.root()
//                .child("Void", "caller")
                .child("Void")
        ;

        PureBigraph redex = builderRedex.create();
        PureBigraph reactum = builderReactum.create();
        if (EXPORT) {
            toPNG(redex, "cap-append-lhs", TARGET_DUMP_PATH);
            toPNG(reactum, "cap-append-rhs", TARGET_DUMP_PATH);
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
        PureBigraphBuilder<DynamicSignature> builderRedex = pureBuilder(createSignature());
        PureBigraphBuilder<DynamicSignature> builderReactum = pureBuilder(createSignature());

        BigraphEntity.InnerName tmp1 = builderRedex.createInner("tmp");
        builderRedex.root()
                .child("this").down().child("thisRef").linkInner(tmp1).site()
        ;
        //
        builderRedex.root()
//                .child("append", "caller").linkInner(tmp1).down().child("Void", "caller")
                .child("append").linkInner(tmp1).down().child("Void")

        ;
        builderRedex.closeInner();


        builderReactum.root()
//                .child("thisRef")
                .child("this").down().site()
        ;
        //
        builderReactum.root()
//                .child("Void", "caller")
                .child("Void")
        ;
        builderReactum.closeInner();

        PureBigraph redex = builderRedex.create();
        PureBigraph reactum = builderReactum.create();
        if (EXPORT) {
            toPNG(redex, "cap-return-lhs", TARGET_DUMP_PATH);
            toPNG(reactum, "cap-return-rhs", TARGET_DUMP_PATH);
        }
        JLibBigBigraphEncoder encoder = new JLibBigBigraphEncoder();
        Bigraph encodedRedex = encoder.encode(redex);
        JLibBigBigraphDecoder decoder = new JLibBigBigraphDecoder();
        PureBigraph decode = decoder.decode(encodedRedex, redex.getSignature());
        if (EXPORT) {
            toPNG(decode, "return_decoded_1", TARGET_DUMP_PATH);
        }
//        RewritingRule rewritingRule = new RewritingRule(encodedRedex, encodedRedex, 0, 1, 2);
        ReactionRule<PureBigraph> rr = new ParametricReactionRule<>(redex, reactum).withLabel("returnRule");
        return rr;
    }

    private DynamicSignature createSignature() {
        DynamicSignatureBuilder defaultBuilder = pureSignatureBuilder();
        defaultBuilder
                .add("append", 1) // as much as we callers have
                .add("main", 0)
                .add("list", 0)
                .add("this", 0)
                .add("thisRef", 1) // as much as we have callers
                .add("Cell", 0)
                .add("Void", 0)
                .add("val", 0)
                .add("N1", 0) // parameterized control
                .add("N2", 0)
                .add("N3", 0)
                .add("N4", 0)
                .add("N5", 0)
                .add("N6", 0)
                .add("next", 0)
        ;
        return defaultBuilder.create();
    }
}
