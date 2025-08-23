package org.bigraphs.grgen.converter.usecase.selfsortingrobots;

import org.apache.commons.io.FileUtils;
import org.bigraphs.framework.converter.bigrapher.BigrapherTransformator;
import org.bigraphs.framework.core.AbstractEcoreSignature;
import org.bigraphs.framework.core.Bigraph;
import org.bigraphs.framework.core.BigraphComposite;
import org.bigraphs.framework.core.BigraphFileModelManagement;
import org.bigraphs.framework.core.exceptions.IncompatibleSignatureException;
import org.bigraphs.framework.core.exceptions.InvalidConnectionException;
import org.bigraphs.framework.core.exceptions.operations.IncompatibleInterfaceException;
import org.bigraphs.framework.core.impl.BigraphEntity;
import org.bigraphs.framework.core.impl.elementary.Linkings;
import org.bigraphs.framework.core.impl.elementary.Placings;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.pure.PureBigraphBuilder;
import org.bigraphs.framework.core.impl.signature.DefaultDynamicSignature;
import org.bigraphs.framework.core.impl.signature.DynamicSignatureBuilder;
import org.bigraphs.framework.core.reactivesystem.*;
import org.bigraphs.framework.core.utils.BigraphUtil;
import org.bigraphs.framework.simulation.matching.pure.PureReactiveSystem;
import org.bigraphs.framework.simulation.modelchecking.BigraphModelChecker;
import org.bigraphs.framework.simulation.modelchecking.ModelCheckingOptions;
import org.bigraphs.framework.simulation.modelchecking.PureBigraphModelChecker;
import org.bigraphs.framework.visualization.ReactionGraphExporter;
import org.bigraphs.framework.visualization.SwingGraphStreamer;
import org.bigraphs.grgen.converter.BigraphUnitTestSupport;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.graphstream.ui.view.Viewer;
import org.jgrapht.Graph;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.bigraphs.framework.core.factory.BigraphFactory.*;
import static org.bigraphs.framework.simulation.modelchecking.ModelCheckingOptions.transitionOpts;

/**
 * This class models a cyber-physical scenario in which a set of
 * autonomous robots coordinate to self-sort on a shared physical grid.
 * The modeling approach follows the transition-system–based specification
 * described in [1] but in bigraph-jargon.
 * <p>
 * The purpose of this benchmark is to study the state-space explosion
 * problem in cyber-physical systems (CPS) and to illustrate how this
 * specific CPS scenario can be modeled and analyzed using bigraphs.
 * <p>
 * Usage: This class is annotated for JMH benchmarking. Results
 * provide average execution times (in nanoseconds) for the
 * self-sorting coordination protocol, under different runtime
 * configurations.
 *
 * @author Dominik Grzelak
 * @see <a href="https://doi.org/10.1007/978-3-031-43345-0_7">
 * Lion, Arbab, Talcott (2023)</a>
 */
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(value = 2) // , jvmArgs = {"-Xms2G", "-Xmx2G"}
@Warmup(iterations = 50, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 30, time = 5, timeUnit = TimeUnit.SECONDS)
@Disabled
public class SelfSortingRobots implements BigraphUnitTestSupport {
    private final static String SOURCE_MODEL_PATH = "src/test/resources/models/selfsortingrobots/";
    private final static String TARGET_DUMP_PATH = "src/test/resources/dump/selfsortingrobots/";
    private final static String TARGET_SAMPLE_PATH_FORMAT = "sample/tmp/selfsortingrobots-n%s/";

    private final static boolean AUTO_CLEAN_BEFORE = true;
    private final static boolean EXPORT = true;

    private final int roboCountTotal = 3;
    private final String bigridPatternModelFile = "2x" + roboCountTotal + "_unidirectional";
//    private final String initMvmtPatternGrid = "2x" + roboCountTotal; // other variant

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SelfSortingRobots.class.getSimpleName())
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
    void export_models() throws Exception {
        String TARGET_SAMPLE_PATH = String.format(TARGET_SAMPLE_PATH_FORMAT, roboCountTotal);
//        // Instantiate bigraph models
        DefaultDynamicSignature sig = getCombinedSystemSignature();
        BigraphFileModelManagement.Store.exportAsInstanceModel(sig, new FileOutputStream(TARGET_SAMPLE_PATH + "sig.xmi"), "signatureMetaModel.ecore");
        BigraphFileModelManagement.Store.exportAsMetaModel(sig, new FileOutputStream(TARGET_SAMPLE_PATH + "signatureMetaModel.ecore"));
        PureBigraph agent = createAgent(roboCountTotal, bigridPatternModelFile); //specify the number of processes here
        BigraphFileModelManagement.Store.exportAsInstanceModel(agent, new FileOutputStream(TARGET_SAMPLE_PATH + "host.xmi"), "bigraphMetaModel.ecore");
        BigraphFileModelManagement.Store.exportAsMetaModel(agent, new FileOutputStream(TARGET_SAMPLE_PATH + "bigraphMetaModel.ecore"));
        if (EXPORT) {
            eb(agent, "agent", TARGET_DUMP_PATH);
        }

        // Rules
        // Create all sync rules for robots satisfying i > j
        List<ReactionRule> collectedRules = new ArrayList<>();
        for (int i = 0; i < roboCountTotal; i++) {
            for (int j = 0; j < i; j++) {
                ReactionRule<PureBigraph> startSync = startSync(i, j);
                System.out.println(startSync.getLabel());
                collectedRules.add(startSync);
//                exportToJson(TARGET_SAMPLE_PATH + startSync.getLabel() + "-map.json", startSync.getLabel(), startSync.getTrackingMap());
                BigraphFileModelManagement.Store.exportAsInstanceModel(startSync.getRedex(), new FileOutputStream(TARGET_SAMPLE_PATH + startSync.getLabel() + "-lhs.xmi"), "bigraphMetaModel.ecore");
                BigraphFileModelManagement.Store.exportAsInstanceModel(startSync.getReactum(), new FileOutputStream(TARGET_SAMPLE_PATH + startSync.getLabel() + "-rhs.xmi"), "bigraphMetaModel.ecore");
            }
        }


        ReactionRule<PureBigraph> initMovePatRule = initMovePattern();
        collectedRules.add(initMovePatRule);
        BigraphFileModelManagement.Store.exportAsInstanceModel(initMovePatRule.getRedex(), new FileOutputStream(TARGET_SAMPLE_PATH + initMovePatRule.getLabel() + "-lhs.xmi"), "bigraphMetaModel.ecore");
        BigraphFileModelManagement.Store.exportAsInstanceModel(initMovePatRule.getReactum(), new FileOutputStream(TARGET_SAMPLE_PATH + initMovePatRule.getLabel() + "-rhs.xmi"), "bigraphMetaModel.ecore");

        ReactionRule<PureBigraph> moveRobot = moveRobotWaypoint();
        collectedRules.add(moveRobot);
        BigraphFileModelManagement.Store.exportAsInstanceModel(moveRobot.getRedex(), new FileOutputStream(TARGET_SAMPLE_PATH + moveRobot.getLabel() + "-lhs.xmi"), "bigraphMetaModel.ecore");
        BigraphFileModelManagement.Store.exportAsInstanceModel(moveRobot.getReactum(), new FileOutputStream(TARGET_SAMPLE_PATH + moveRobot.getLabel() + "-rhs.xmi"), "bigraphMetaModel.ecore");

        for (int i = 0; i < roboCountTotal; i++) {
            for (int j = i + 1; j < roboCountTotal; j++) {
                ReactionRule<PureBigraph> endSync = endSync(i, j);
                System.out.println(endSync.getLabel());
                collectedRules.add(endSync);
                BigraphFileModelManagement.Store.exportAsInstanceModel(endSync.getRedex(), new FileOutputStream(TARGET_SAMPLE_PATH + endSync.getLabel() + "-lhs.xmi"), "bigraphMetaModel.ecore");
                BigraphFileModelManagement.Store.exportAsInstanceModel(endSync.getReactum(), new FileOutputStream(TARGET_SAMPLE_PATH + endSync.getLabel() + "-rhs.xmi"), "bigraphMetaModel.ecore");
            }
        }

        exportAllToJson(collectedRules.stream().map(ReactionRule::getTrackingMap).collect(Collectors.toSet()),
                TARGET_SAMPLE_PATH + "map.json");
    }

    @Test
    @Benchmark
//    @Fork(value = 2, warmups = 50)
//    @BenchmarkMode(Mode.Throughput)
    public void simulate() throws Exception {
        PureReactiveSystem reactiveSystem = new PureReactiveSystem();
        PureBigraph agent = createAgent(roboCountTotal, bigridPatternModelFile);
        if (EXPORT) {
            eb(agent, "agent", TARGET_DUMP_PATH);
        }
        reactiveSystem.setAgent(agent);
        SwingGraphStreamer graphStreamer = new SwingGraphStreamer(agent)
                .renderSites(false)
                .renderRoots(false);
        graphStreamer.prepareSystemEnvironment();
        Viewer graphViewer = graphStreamer.getGraphViewer();
        while (graphViewer != null) {
            Thread.sleep(100);
        }
        // Create all sync rules for robots satisfying i > j
        for (int i = 0; i < roboCountTotal; i++) {
            for (int j = 0; j < i; j++) {
                System.out.println("startSync(" + i + ", " + j + ")");
                ReactionRule<PureBigraph> startSync = startSync(i, j);
                reactiveSystem.addReactionRule(startSync);
            }
        }

        ReactionRule<PureBigraph> initMovePatRule = initMovePattern();
        reactiveSystem.addReactionRule(initMovePatRule);

        ReactionRule<PureBigraph> moveRobot = moveRobotWaypoint();
        reactiveSystem.addReactionRule(moveRobot);

        for (int i = 0; i < roboCountTotal; i++) {
            for (int j = i + 1; j < roboCountTotal; j++) {
                System.out.println("endSync(" + i + ", " + j + ")");
                ReactionRule<PureBigraph> endSync = endSync(i, j);
                reactiveSystem.addReactionRule(endSync);
            }
        }

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


        Graph<ReactionGraph.LabeledNode, ReactionGraph.LabeledEdge> graph = modelChecker.getReactionGraph().getGraph();
        ReactionGraphStats<PureBigraph> graphStats = modelChecker.getReactionGraph().getGraphStats();
        System.out.println("Edges: " + graphStats.getTransitionCount());
        System.out.println("Vertices: " + graphStats.getStateCount());
        ReactionGraphExporter rge = new ReactionGraphExporter(reactiveSystem);
        rge.toPNG(modelChecker.getReactionGraph(), new File(TARGET_DUMP_PATH + "reaction-graph.dot"));
        System.out.println("Output written to " + TARGET_DUMP_PATH);

        ReactionGraph.LabeledNode vertexWithNoOutgoingEdges;
        for (ReactionGraph.LabeledNode vertex : graph.vertexSet()) {
            // Check if the vertex has no outgoing edges
            if (graph.outgoingEdgesOf(vertex).isEmpty()) {
                vertexWithNoOutgoingEdges = vertex;
                System.out.println("vertexWithNoOutgoingEdges = " + vertexWithNoOutgoingEdges);
//                break; // Exit loop once we find a vertex with no outgoing edges
            }
        }

//        ReactionGraphAnalysis<PureBigraph> analysis = ReactionGraphAnalysis.createInstance();
//        List<ReactionGraphAnalysis.StateTrace<PureBigraph>> pathsToLeaves = analysis.findAllPathsInGraphToLeaves(modelChecker.getReactionGraph());
//        System.out.println(pathsToLeaves.size());
//        pathsToLeaves.get(0).getStateLabels().size();
//        int minSize = Integer.MAX_VALUE; // Start with a large value
//        ReactionGraphAnalysis.StateTrace<PureBigraph> smallestTrace = null;
//        for (ReactionGraphAnalysis.StateTrace<PureBigraph> trace : pathsToLeaves) {
//            int currentSize = trace.getStateLabels().size();
//            if (currentSize < minSize) {
//                minSize = currentSize;
//                smallestTrace = trace;
//                System.out.println("Smallest entry: " + smallestTrace.getStateLabels());
//            }
//        }
//        // After the loop, smallestTrace holds the entry with the smallest size in getStateLabels()
//        System.out.println("Smallest entry has size: " + minSize);
//        System.out.println("Smallest entry: " + smallestTrace.getStateLabels());

    }

    public ModelCheckingOptions setUpSimOpts() {
        Path completePath = Paths.get(TARGET_DUMP_PATH, "transition_graph.png");
        ModelCheckingOptions opts = ModelCheckingOptions.create();
        opts
                .and(transitionOpts()
                                .setMaximumTransitions(1000000)
                                .setMaximumTime(-1)
//                        .rewriteOpenLinks(false)
                                .allowReducibleClasses(true)
                                .create()
                )
//                .withParallelRuleMatching(true)
                .doMeasureTime(false)
                .and(ModelCheckingOptions.exportOpts()
                        .setReactionGraphFile(new File(completePath.toUri()))
                        .setPrintCanonicalStateLabel(false)
                        // .setFormatsEnabled(List.of(ModelCheckingOptions.ExportOptions.Format.PNG))
                        // .setFormatsEnabled(List.of(ModelCheckingOptions.ExportOptions.Format.XMI))
                        .setFormatsEnabled(List.of(ModelCheckingOptions.ExportOptions.Format.XMI, ModelCheckingOptions.ExportOptions.Format.PNG))
                        .setOutputStatesFolder(new File(TARGET_DUMP_PATH + "states/"))
                        .create()
                )
        ;
        return opts;
    }

    public PureBigraph createAgent(int roboCountTotal, String gridFile) throws Exception {
        //bigrid unidirectional_forward
        // * -> * -> ...
        // |    |
        // \/   \/
        // * -> * ->  ...
        // Build the agent: [grid] x [robot array]

        PureBigraph bigrid = createBigrid(gridFile);
        PureBigraph initRobot = initRobots(bigrid.getSites().size(), roboCountTotal, 2 * roboCountTotal - 1);
        PureBigraph agent = ops(bigrid).nesting(initRobot).getOuterBigraph();
        return agent;
    }

    public PureBigraph createBigrid(String gridSize) throws Exception {
        DefaultDynamicSignature mergedSig = getCombinedSystemSignature();
        EPackage gridEPackage = createOrGetBigraphMetaModel(mergedSig);
        List<EObject> girdEObjects = BigraphFileModelManagement.Load.bigraphInstanceModel(gridEPackage,
                SOURCE_MODEL_PATH + "grid-" + gridSize + ".xmi"  // "grid-2x5.xmi"
        );

        PureBigraph gridComponent = BigraphUtil.toBigraph(gridEPackage, girdEObjects.get(0), mergedSig);
        if (EXPORT) {
//            eb(gridComponent, "agent_gridComponent", TARGET_DUMP_PATH);
        }

        return gridComponent;
    }

    public PureBigraph initRobots(int paramSitesTotal, int roboCountTotal, int endRowiseIx) throws Exception {
        DefaultDynamicSignature sig = getCombinedSystemSignature();
        Placings<DefaultDynamicSignature> placingsBuilder = purePlacings(sig);
        Linkings<DefaultDynamicSignature> linkings = pureLinkings(sig);
        List<Bigraph> collects = IntStream.range(0, paramSitesTotal)
                .mapToObj(ix ->
//                        (Bigraph) placingsBuilder.barren()
                                pureBuilder(sig).createRoot().addChild("OccupiedBy").createBigraph()
                )
                .collect(Collectors.toList());

        int batterPowerTotal = 2;
        for (int rID = 0; rID < roboCountTotal; rID++) {
            PureBigraph roboTmp = buildRobot("N" + rID, batterPowerTotal);
            collects.set(endRowiseIx - rID, roboTmp);
//            if (EXPORT) {
//                eb(roboTmp, "robo_" + ("N" + rID), TARGET_DUMP_PATH);
//            }
        }

        Bigraph roboPlacements = collects.stream()
                .reduce(linkings.identity_e(), accumulator::apply);

//        if (EXPORT) {
//            eb(roboPlacements, "robo_complete", TARGET_DUMP_PATH);
//        }

        return (PureBigraph) roboPlacements;
    }

    /**
     * @param ID          N1, N2, ... (see controls in signature)
     * @param batterPower number of power (each move decreases power)
     * @return the robot as bigraph
     * @throws InvalidConnectionException
     */
    public PureBigraph buildRobot(String ID, int batterPower) throws InvalidConnectionException, IOException {
        PureBigraphBuilder<DefaultDynamicSignature> b = pureBuilder(getCombinedSystemSignature());
        PureBigraphBuilder<DefaultDynamicSignature>.Hierarchy bat = b.hierarchy("Bat");
        for (int i = 0; i < batterPower; i++) {
            bat.addChild("Pow");
        }
        b.createRoot()
                /**/.addChild("OccupiedBy").down()
                /**//**/.addChild("Robot", ("R" + ID).toLowerCase()).down()
                /**//**//**/.addChild("ID").down().addChild(ID).up()
//                /**//**//**/.addChild("Bat").down().addChild("Pow").up()
                /**//**//**/.addChild(bat)
                /**//**//**/.addChild("SLck")
                /**//**//**/.addChild("Mvmt")
        ;

        return b.createBigraph();
    }

    public PureBigraph buildSyncedRobotTemplate(String roboChannel, String syncRefLbl, int mvmtToken) throws InvalidConnectionException, IOException {
        PureBigraphBuilder<DefaultDynamicSignature> b = pureBuilder(getCombinedSystemSignature());
        PureBigraphBuilder<DefaultDynamicSignature>.Hierarchy mvmt = b.hierarchy("Mvmt");
        if (mvmtToken > 0) {
            for (int i = 0; i < mvmtToken; i++) {
                mvmt.addChild("Token");
            }
        }
        mvmt.top();
        b.createRoot()
                /**/.addSite()
                /**/.addChild("OccupiedBy").down()
                /**/.addChild("Robot", roboChannel.toLowerCase()).down()
                /**//**/.addSite()
                /**//**/.addChild("SLck").down().addChild("SLckRef", syncRefLbl).up()
//                /**//**/.addChild("Mvmt")
                /**//**/.addChild(mvmt)
        ;

        return b.createBigraph();
    }

    public ReactionRule<PureBigraph> startSync(int roboID_left, int roboID_right) throws Exception {
        if (roboID_right >= roboID_left) {
            throw new Exception("Rule violates condition i > j. The left robot must have a greater ID than the right robot.");
        }
        PureBigraphBuilder<DefaultDynamicSignature> bLHS = pureBuilder(getCombinedSystemSignature());
        PureBigraphBuilder<DefaultDynamicSignature> bRHS = pureBuilder(getCombinedSystemSignature());

        // LHS
        bLHS.createRoot()
                /**/.addChild("Locale", "left").down()
                /**/.addSite()
                /**/.addChild("Route", "right")
                /**/.addChild("OccupiedBy").down()
                /**//**/.addChild("Robot", ("RN" + roboID_left).toLowerCase()).down()
                /**//**//**/.addChild("ID").down().addChild("N" + roboID_left).up()
                /**//**//**/.addChild("SLck")
                /**//**//**/.addChild("Mvmt")
                /**//**//**/.addSite().top()
                /**/
                /**/.addChild("Locale", "right").down()
                /**/.addSite()
//                /**/.addChild("Route", "left")
                /**/.addChild("OccupiedBy").down()
                /**//**/.addChild("Robot", ("RN" + roboID_right).toLowerCase()).down()
                /**//**//**/.addChild("ID").down().addChild("N" + roboID_right).up()
                /**//**//**/.addChild("SLck")
                /**//**//**/.addChild("Mvmt")
                /**//**//**/.addSite().top()
        ;

        // RHS
        bRHS.createRoot()
                /**/.addChild("Locale", "left").down()
                /**/.addSite()
                /**/.addChild("Route", "right")
                /**/.addChild("OccupiedBy").down()
                /**//**/.addChild("Robot", ("RN" + roboID_left).toLowerCase()).down()
                /**//**//**/.addChild("ID").down().addChild("N" + roboID_left).up()
                /**//**//**/.addChild("SLck").down().addChild("SLckRef").linkToInner("x").up()
                /**//**//**/.addChild("Mvmt")
                /**//**//**/.addSite().top()
                /**/
                /**/.addChild("Locale", "right").down()
                /**/.addSite()
//                /**/.addChild("Route", "left")
                /**/.addChild("OccupiedBy").down()
                /**//**/.addChild("Robot", ("RN" + roboID_right).toLowerCase()).down()
                /**//**//**/.addChild("ID").down().addChild("N" + roboID_right).up()
                /**//**//**/.addChild("SLck").down().addChild("SLckRef").linkToInner("x").up()
                /**//**//**/.addChild("Mvmt")
                /**//**//**/.addSite().top()
        ;
        bRHS.closeInnerName(bRHS.createInnerName("x"));

        PureBigraph redex = bLHS.createBigraph();
        PureBigraph reactum = bRHS.createBigraph();
        if (EXPORT) {
            eb(redex, "startSync-lhs_" + roboID_left + "_" + roboID_right, TARGET_DUMP_PATH);
            eb(reactum, "startSync-rhs_" + roboID_left + "_" + roboID_right, TARGET_DUMP_PATH);
        }

        TrackingMap trckMap = new TrackingMap();
        trckMap.put("v0", "v0");
        trckMap.put("v1", "v1");
        trckMap.put("v2", "v2");
        trckMap.put("v3", "v3");
        trckMap.put("v4", "v4");
        trckMap.put("v5", "v5");
        trckMap.put("v6", "v6");
        trckMap.put("v7", "");
        trckMap.put("v8", "v7");
        trckMap.put("v9", "v8");
        trckMap.put("v10", "v9");
        trckMap.put("v11", "v10");
        trckMap.put("v12", "v11");
        trckMap.put("v13", "v12");
        trckMap.put("v14", "v13");
        trckMap.put("v15", "");
        trckMap.put("v16", "v14");
        trckMap.put("e0", "");
        reactum.getOuterNames().forEach(x -> {
            trckMap.put(x.getName(), x.getName());
        });
        List<String> links = new ArrayList<>(reactum.getOuterNames().stream().map(BigraphEntity.Link::getName).toList());
        links.add("e0");
        trckMap.addLinkNames(links.toArray(new String[0]));

        ReactionRule<PureBigraph> rr = new ParametricReactionRule<>(redex, reactum)
                .withLabel("ss_" + roboID_left + "_" + roboID_right)
                .withTrackingMap(trckMap);
        trckMap.setRuleName(rr.getLabel());
        return rr;

        //  JLibBigBigraphEncoder encoder = new JLibBigBigraphEncoder();
        //  JLibBigBigraphDecoder decoder = new JLibBigBigraphDecoder();
        //  Bigraph encodedRedex = encoder.encode(redex);
        //  Bigraph encodedReactum = encoder.encode(reactum, encodedRedex.getSignature());
        //  RewritingRule rewritingRule = new RewritingRule(encodedRedex, encodedReactum, 0, 1, 2, 3, 4);
    }

    public ReactionRule<PureBigraph> endSync(int roboID_left, int roboID_right) throws Exception {
        if (roboID_left > roboID_right) {
            throw new Exception("Rule violates condition i > j. The left robot must have a lower ID than the right robot.");
        }
        PureBigraphBuilder<DefaultDynamicSignature> bLHS = pureBuilder(getCombinedSystemSignature());
        PureBigraphBuilder<DefaultDynamicSignature> bRHS = pureBuilder(getCombinedSystemSignature());

        // LHS
        bLHS.createRoot()
                /**/.addChild("Locale", "left").down()
                /**/.addSite()
                /**/.addChild("Route", "right")
                /**/.addChild("OccupiedBy").down()
                /**//**/.addChild("Robot", ("RN" + roboID_left).toLowerCase()).down()
                /**//**//**/.addChild("ID").down().addChild("N" + roboID_left).up()
                /**//**//**/.addChild("SLck").down().addChild("SLckRef").linkToInner("x").up()
                /**//**//**/.addChild("Mvmt")
                /**//**//**/.addSite().top()
                /**/
                /**/.addChild("Locale", "right").down()
                /**/.addSite()
//                /**/.addChild("Route", "left")
                /**/.addChild("OccupiedBy").down()
                /**//**/.addChild("Robot", ("RN" + roboID_right).toLowerCase()).down()
                /**//**//**/.addChild("ID").down().addChild("N" + roboID_right).up()
                /**//**//**/.addChild("SLck").down().addChild("SLckRef").linkToInner("x").up()
                /**//**//**/.addChild("Mvmt")
                /**//**//**/.addSite().top()
        ;
        bLHS.closeInnerName(bLHS.createInnerName("x"));
        // RHS
        bRHS.createRoot()
                /**/.addChild("Locale", "left").down()
                /**/.addSite()
                /**/.addChild("Route", "right")
                /**/.addChild("OccupiedBy").down()
                /**//**/.addChild("Robot", ("RN" + roboID_left).toLowerCase()).down()
                /**//**//**/.addChild("ID").down().addChild("N" + roboID_left).up()
                /**//**//**/.addChild("SLck")
                /**//**//**/.addChild("Mvmt")
                /**//**//**/.addSite().top()
                /**/
                /**/.addChild("Locale", "right").down()
                /**/.addSite()
                /**/.addChild("OccupiedBy").down()
                /**//**/.addChild("Robot", ("RN" + roboID_right).toLowerCase()).down()
                /**//**//**/.addChild("ID").down().addChild("N" + roboID_right).up()
                /**//**//**/.addChild("SLck")
                /**//**//**/.addChild("Mvmt")
                /**//**//**/.addSite().top()
        ;

        PureBigraph redex = bLHS.createBigraph();
        PureBigraph reactum = bRHS.createBigraph();
        if (EXPORT) {
            eb(redex, "endSync-lhs_" + roboID_left + "_" + roboID_right, TARGET_DUMP_PATH);
            eb(reactum, "endSync-rhs_" + roboID_left + "_" + roboID_right, TARGET_DUMP_PATH);
        }

        TrackingMap trckMap = new TrackingMap();
        trckMap.put("v0", "v0");
        trckMap.put("v1", "v1");
        trckMap.put("v2", "v2");
        trckMap.put("v3", "v3");
        trckMap.put("v4", "v4");
        trckMap.put("v5", "v5");
        trckMap.put("v6", "v6");
        trckMap.put("v7", "v8");

        trckMap.put("v8", "v9");
        trckMap.put("v9", "v10");
        trckMap.put("v10", "v11");
        trckMap.put("v11", "v12");
        trckMap.put("v12", "v13");
        trckMap.put("v13", "v14");
        trckMap.put("v14", "v16");
        reactum.getOuterNames().forEach(x -> {
            trckMap.put(x.getName(), x.getName());
        });
        List<String> links = new ArrayList<>(reactum.getOuterNames().stream().map(BigraphEntity.Link::getName).toList());
        trckMap.addLinkNames(links.toArray(new String[0]));

        ReactionRule<PureBigraph> rr = new ParametricReactionRule<>(redex, reactum)
                .withLabel("es_" + roboID_left + "_" + roboID_right)
                .withTrackingMap(trckMap);
        trckMap.setRuleName(rr.getLabel());
        return rr;
    }

    public ReactionRule<PureBigraph> initMovePattern() throws Exception {
        DefaultDynamicSignature sig = getCombinedSystemSignature();
        Placings<DefaultDynamicSignature> placings = purePlacings(sig);
        Linkings<DefaultDynamicSignature> linkings = pureLinkings(sig);
        PureBigraphBuilder<DefaultDynamicSignature> closeSitesRedex = pureBuilder(sig);
        PureBigraphBuilder<DefaultDynamicSignature> builderReactum = pureBuilder(sig);


        PureBigraph bigrid = createBigrid("2x2_unidirectional");
        List<Bigraph> collect = IntStream.range(0, 4)
                .mapToObj(ix ->
                        pureBuilder(sig).createRoot()
                                .addSite()
                                .addChild("OccupiedBy").createBigraph()
                )
                .collect(Collectors.toList());
        collect.set(2, buildSyncedRobotTemplate("N_left", "x", -1));
        collect.set(3, buildSyncedRobotTemplate("N_right", "x", -1));
        Linkings<DefaultDynamicSignature>.Closure cx = linkings.closure("x");
        Bigraph roboPlacements = collect.stream().reduce(linkings.identity_e(), accumulator::apply);
        Linkings<DefaultDynamicSignature>.Identity idOuter = linkings.identity("N_left".toLowerCase(), "N_right".toLowerCase());
        Bigraph<DefaultDynamicSignature> elemGlueBigraph = ops(cx).juxtapose(idOuter).juxtapose(placings.permutation(4)).getOuterBigraph();
        roboPlacements = ops(elemGlueBigraph).compose(roboPlacements).getOuterBigraph();

        // LHS
        //close sites 0,1,3,5
        closeSitesRedex.createRoot();
        closeSitesRedex.createRoot().addSite(); // HERE
        closeSitesRedex.createRoot().addSite(); // 2 robot
        closeSitesRedex.createRoot();
        closeSitesRedex.createRoot().addSite(); // 4 robot
        closeSitesRedex.createRoot().addSite(); // HERE
        BigraphComposite baseRedex = ops(ops(bigrid).nesting(roboPlacements).getOuterBigraph());
        PureBigraph redex = (PureBigraph) baseRedex.compose(closeSitesRedex.createBigraph()).getOuterBigraph();

        // RHS
        collect = IntStream.range(0, 4)
                .mapToObj(ix ->
                        pureBuilder(sig).createRoot()
                                .addSite()
                                .addChild("OccupiedBy").createBigraph()
                )
                .collect(Collectors.toList());
        collect.set(2, buildSyncedRobotTemplate("N_left", "x", 1));
        collect.set(3, buildSyncedRobotTemplate("N_right", "x", 3));
        cx = linkings.closure("x");
        roboPlacements = collect.stream().reduce(linkings.identity_e(), accumulator::apply);
        idOuter = linkings.identity("N_left".toLowerCase(), "N_right".toLowerCase());
        elemGlueBigraph = ops(cx).juxtapose(idOuter).juxtapose(placings.permutation(4)).getOuterBigraph();
        roboPlacements = ops(elemGlueBigraph).compose(roboPlacements).getOuterBigraph();


        builderReactum.createRoot().addChild("WayPoint", "y2")
                .down().addChild("SLckRef", "N_right".toLowerCase());
        builderReactum.createRoot().addChild("WayPoint", "y0")
                .down().addChild("SLckRef", "N_right".toLowerCase()).up()
                .addSite();
        builderReactum.createRoot()
                .addSite();
        builderReactum.createRoot().addChild("WayPoint", "y3")
                .down().addChild("SLckRef", "N_left".toLowerCase());
        builderReactum.createRoot()
                .addSite();
        builderReactum.createRoot().addChild("WayPoint", "y1")
                .down().addChild("SLckRef", "N_right".toLowerCase()).up()
                .addSite()
        ;

        BigraphComposite baseReactum = ops(ops(bigrid).nesting(roboPlacements).getOuterBigraph());
        PureBigraph reactum = (PureBigraph) baseReactum.nesting(builderReactum.createBigraph()).getOuterBigraph();

        if (EXPORT) {
            eb(redex, "initMovePat-lhs", TARGET_DUMP_PATH);
            eb(reactum, "initMovePath-rhs", TARGET_DUMP_PATH);
        }

        TrackingMap trckMap = new TrackingMap();
        trckMap.put("v0", "v0");
        trckMap.put("v1", "v1");
        trckMap.put("v2", "v2");
        trckMap.put("v3", "");
        trckMap.put("v4", "");
        trckMap.put("v5", "v3");

        trckMap.put("v6", "v4");
        trckMap.put("v7", "v5");
        trckMap.put("v8", "");
        trckMap.put("v9", "");
        trckMap.put("v10", "v6");

        trckMap.put("v11", "v7");
        trckMap.put("v12", "v8");
        trckMap.put("v13", "");
        trckMap.put("v14", "");
        trckMap.put("v15", "v9");
        trckMap.put("v16", "v10");
        trckMap.put("v17", "v11");
        trckMap.put("v18", "v12");
        trckMap.put("v19", "v13");
        trckMap.put("v20", "");

        trckMap.put("v21", "v14");
        trckMap.put("v22", "");
        trckMap.put("v23", "");
        trckMap.put("v24", "v15");
        trckMap.put("v25", "v16");
        trckMap.put("v26", "v17");
        trckMap.put("v27", "v18");
        trckMap.put("v28", "v19");
        trckMap.put("v29", "");
        trckMap.put("v30", "");
        trckMap.put("v31", "");
        trckMap.put("e0", "e0");
        reactum.getOuterNames().forEach(x -> {
            trckMap.put(x.getName(), x.getName());
        });
        List<String> links = new ArrayList<>(reactum.getOuterNames().stream().map(BigraphEntity.Link::getName).toList());
        links.add("e0");
        trckMap.addLinkNames(links.toArray(new String[0]));

        ReactionRule<PureBigraph> rr = new ParametricReactionRule<>(redex, reactum)
                .withLabel("initMvmt")
                .withTrackingMap(trckMap);
        trckMap.setRuleName(rr.getLabel());
        return rr;
//        JLibBigBigraphEncoder encoder = new JLibBigBigraphEncoder();
//        JLibBigBigraphDecoder decoder = new JLibBigBigraphDecoder();
//        Bigraph encodedRedex = encoder.encode(redex);
//        Bigraph encodedReactum = encoder.encode(reactum, encodedRedex.getSignature());
//        RewritingRule rewritingRule = new RewritingRule(encodedRedex, encodedReactum, 0, 1, 2);
    }


    public ReactionRule<PureBigraph> moveRobotWaypoint() throws Exception {
        DefaultDynamicSignature sig = getCombinedSystemSignature();
        PureBigraphBuilder<DefaultDynamicSignature> bLHS = pureBuilder(sig);
        PureBigraphBuilder<DefaultDynamicSignature> bRHS = pureBuilder(sig);

        //LHS
        bLHS.createRoot()
                /**/.addChild("Locale", "src").down()
                /**/.addSite()
                /**/.addChild("WayPoint", "tgt").down().addChild("SLckRef", "n_id").up()
                /**/.addChild("OccupiedBy").down()
                /**//**/.addChild("Robot", "n_id").down()
                /**//**//**/.addChild("SLck").down().addChild("SLckRef", "ref").up()
                /**//**//**/.addChild("Mvmt").down().addChild("Token").addSite().up()
                /**//**//**/.addSite().top()
                /**/
                /**/.addChild("Locale", "tgt").down()
                /**/.addSite()
                /**/.addChild("OccupiedBy")
        ;
        PureBigraph redex = bLHS.createBigraph();

        //RHS
        bRHS.createRoot()
                /**/.addChild("Locale", "src").down()
                /**/.addSite()
                /**/.addChild("OccupiedBy").top()
                /**/
                /**/.addChild("Locale", "tgt").down()
                /**/.addChild("OccupiedBy").down()
                /**//**/.addChild("Robot", "n_id").down()
                /**//**//**/.addChild("SLck").down().addChild("SLckRef", "ref").up()
                /**//**//**/.addChild("Mvmt").down().addSite().up()
                /**//**//**/.addSite().up().up()
                /**/.addSite()
        ;
        PureBigraph reactum = bRHS.createBigraph();
        if (EXPORT) {
            eb(redex, "moveRobot-lhs", TARGET_DUMP_PATH);
            eb(reactum, "moveRobot-rhs", TARGET_DUMP_PATH);
        }

        TrackingMap trckMap = new TrackingMap();
        trckMap.put("v0", "v0");
        trckMap.put("v1", "v3");

        trckMap.put("v2", "v9");
        trckMap.put("v3", "v10");
        trckMap.put("v4", "v4");
        trckMap.put("v5", "v5");
        trckMap.put("v6", "v6");
        trckMap.put("v7", "v7");
        reactum.getOuterNames().forEach(x -> {
            trckMap.put(x.getName(), x.getName());
        });
        List<String> links = new ArrayList<>(reactum.getOuterNames().stream().map(BigraphEntity.Link::getName).toList());
        trckMap.addLinkNames(links.toArray(new String[0]));

        ReactionRule<PureBigraph> rr = new ParametricReactionRule<>(redex, reactum)
                .withLabel("move")
                .withTrackingMap(trckMap);
        trckMap.setRuleName(rr.getLabel());
        return rr;
    }

    public DefaultDynamicSignature getCombinedSystemSignature() throws IOException {
        // EPackage sigEPackage = BigraphFileModelManagement.Load.signatureMetaModel(SOURCE_MODEL_PATH + "mm_sig_loc.ecore");
        List<EObject> sigEObjects = BigraphFileModelManagement.Load.signatureInstanceModel(SOURCE_MODEL_PATH + "mm_sig_loc.ecore", SOURCE_MODEL_PATH + "sig_loc.xmi");
        AbstractEcoreSignature<?> sig = createOrGetSignature(sigEObjects.get(0));
        DefaultDynamicSignature mergedSig = BigraphUtil.mergeSignatures((DefaultDynamicSignature) sig, createRobotSignature());
        return mergedSig;
    }

    public DefaultDynamicSignature createRobotSignature() {
        DynamicSignatureBuilder defaultBuilder = pureSignatureBuilder();
        defaultBuilder
                .addControl("WayPoint", 1)
                .addControl("OccupiedBy", 0)
                .addControl("Robot", 1)
                .addControl("ID", 0)
                .addControl("N0", 0)
                .addControl("N1", 0)
                .addControl("N2", 0)
                .addControl("N3", 0)
                .addControl("N4", 0)
                .addControl("N5", 0)
                .addControl("N6", 0)
                .addControl("N7", 0)
                .addControl("N8", 0)
                .addControl("N9", 0)
                .addControl("N10", 0)
                .addControl("Bat", 0)
                .addControl("Pow", 0)
                .addControl("SLck", 0)
                .addControl("SLckRef", 1)
                .addControl("Mvmt", 0)
                .addControl("Token", 0)
        ;
        return defaultBuilder.create();
    }

    public static BinaryOperator<Bigraph<DefaultDynamicSignature>> accumulator = (partial, element) -> {
        try {
            return ops(partial).parallelProduct(element).getOuterBigraph();
        } catch (IncompatibleSignatureException | IncompatibleInterfaceException e) {
            return pureLinkings(partial.getSignature()).identity_e();
        }
    };

    public static void exportToJson(String filePath, String ruleName, TrackingMap trackingMap) {
        JSONObject rootObject = new JSONObject();

        // Collect data for each rule
        JSONObject ruleObject = new JSONObject();

        // Convert map entries to JSONArray
        JSONArray mapArray = new JSONArray();
        for (Map.Entry<String, String> entry : trackingMap.entrySet()) {
            JSONArray pair = new JSONArray();
            pair.put(entry.getKey());
            pair.put(entry.getValue());
            mapArray.put(pair);
        }
        ruleObject.put("map", mapArray);

        // Convert links to JSONArray
        JSONArray linksArray = new JSONArray(trackingMap.getLinks());
        ruleObject.put("links", linksArray);

        // Add the rule to the root JSON object with the rule name as the key
        rootObject.put(ruleName, ruleObject);

        // Write JSON object to file
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            fileWriter.write(rootObject.toString(4)); // Pretty print with 4-space indentation
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void exportAllToJson(Set<TrackingMap> trackingMaps, String filePath) {
        JSONObject rootObject = new JSONObject();

        for (TrackingMap trackingMap : trackingMaps) {
            JSONObject ruleObject = new JSONObject();

            // Convert map entries to JSONArray
            JSONArray mapArray = new JSONArray();
            for (Map.Entry<String, String> entry : trackingMap.entrySet()) {
                JSONArray pair = new JSONArray();
                pair.put(entry.getKey());
                pair.put(entry.getValue());
                mapArray.put(pair);
            }
            ruleObject.put("map", mapArray);

            // Convert links to JSONArray
            JSONArray linksArray = new JSONArray(trackingMap.getLinks());
            ruleObject.put("links", linksArray);

            // Add the rule object to the root JSON object with the rule name as the key
            rootObject.put(trackingMap.getRuleName(), ruleObject);
        }

        // Write the root JSON object to file
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            fileWriter.write(rootObject.toString(4)); // Pretty print with 4-space indentation
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
