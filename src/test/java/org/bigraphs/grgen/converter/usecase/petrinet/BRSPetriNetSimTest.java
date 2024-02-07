package org.bigraphs.grgen.converter.usecase.petrinet;

import org.bigraphs.framework.core.BigraphFileModelManagement;
import org.bigraphs.framework.core.exceptions.InvalidConnectionException;
import org.bigraphs.framework.core.exceptions.InvalidReactionRuleException;
import org.bigraphs.framework.core.exceptions.ReactiveSystemException;
import org.bigraphs.framework.core.exceptions.builder.TypeNotExistsException;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.signature.DefaultDynamicSignature;
import org.bigraphs.framework.core.reactivesystem.ReactionRule;
import org.bigraphs.framework.simulation.exceptions.BigraphSimulationException;
import org.bigraphs.framework.simulation.matching.pure.PureReactiveSystem;
import org.bigraphs.framework.simulation.modelchecking.BigraphModelChecker;
import org.bigraphs.framework.simulation.modelchecking.ModelCheckingOptions;
import org.bigraphs.framework.simulation.modelchecking.PureBigraphModelChecker;
import org.bigraphs.grgen.converter.BigraphUnitTestSupport;
import org.bigraphs.grgen.converter.demo.DemoBigraphProvider;
import org.bigraphs.grgen.converter.demo.DemoRuleProvider;
import org.bigraphs.grgen.converter.demo.DemoSignatureProvider;
import org.junit.jupiter.api.Disabled;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.bigraphs.framework.simulation.modelchecking.ModelCheckingOptions.transitionOpts;

/**
 * This test generates a transition system from a bigraphical reactive system specification resembling a simplified Petri Net.
 * The output is intended for comparison with the results of GrGen.NET.
 *
 * @author Dominik Grzelak
 */
public class BRSPetriNetSimTest implements BigraphUnitTestSupport {

    private final static String TARGET_DUMP_PATH = "src/test/resources/dump/petrinet/";
    private final static String TARGET_DUMP_PATH2 = "src/test/resources/dump/smarthome/";

    @BeforeClass
    static void setUp() throws IOException {
        File dump = new File(TARGET_DUMP_PATH);
        dump.mkdirs();
        File dump2 = new File(TARGET_DUMP_PATH2);
        dump2.mkdirs();
//        FileUtils.cleanDirectory(new File(TARGET_DUMP_PATH));
        new File(TARGET_DUMP_PATH + "states/").mkdir();
        new File(TARGET_DUMP_PATH2 + "states/").mkdir();
    }

    // because of the canonical encoding of bigraphs and without sorts, the transition graph has length 2 instead of three.
    // (or introduce a pre-place and post-place, or set allowReducibleClasses = false)
    @Test
    @Disabled
    public void testSimulatePetriNet() throws InvalidConnectionException, TypeNotExistsException, IOException, InvalidReactionRuleException, ReactiveSystemException, BigraphSimulationException {
        DemoSignatureProvider signatureProvider = DemoSignatureProvider.getInstance();
        DefaultDynamicSignature sig = signatureProvider.petriNet();
        DemoBigraphProvider bigraphProvider = DemoBigraphProvider.getInstance();
        PureBigraph bigraph = bigraphProvider.petriNet(sig);
//        PureBigraph bigraph = bigraphProvider.petriNetOpenLinks(sig);
        BigraphFileModelManagement.Store.exportAsInstanceModel(bigraph, System.out);
        BigraphFileModelManagement.Store.exportAsMetaModel(bigraph, System.out);
        eb(bigraph, "agent", TARGET_DUMP_PATH, true);

        DemoRuleProvider ruleProvider = DemoRuleProvider.getInstance();
//        ReactionRule<PureBigraph> rr = ruleProvider.petriNetFireRule(sig);
//        ReactionRule<PureBigraph> rr = ruleProvider.petriNetParallelRule(sig);
        ReactionRule<PureBigraph> rr = ruleProvider.petriNetAddRule(sig);
        BigraphFileModelManagement.Store.exportAsInstanceModel(rr.getRedex(), System.out);
        BigraphFileModelManagement.Store.exportAsInstanceModel(rr.getReactum(), System.out);
        eb(rr.getRedex(), "redex", TARGET_DUMP_PATH, true);
        eb(rr.getReactum(), "reactum", TARGET_DUMP_PATH, true);

        PureReactiveSystem reactiveSystem = new PureReactiveSystem();
        reactiveSystem.setAgent(bigraph);
        reactiveSystem.addReactionRule(rr);

        PureBigraphModelChecker modelChecker = new PureBigraphModelChecker(
                reactiveSystem,
                BigraphModelChecker.SimulationStrategy.Type.BFS,
                opts(TARGET_DUMP_PATH));
//        modelChecker.setReactiveSystemListener(this);
        modelChecker.execute();
    }

    @Test
    @Disabled
    public void testSimulateSmartHome() throws Exception {
        DemoSignatureProvider signatureProvider = DemoSignatureProvider.getInstance();
        DefaultDynamicSignature sig = signatureProvider.smartHome();
        DemoBigraphProvider bigraphProvider = DemoBigraphProvider.getInstance();
        PureBigraph bigraph = bigraphProvider.smartHome(sig);

        BigraphFileModelManagement.Store.exportAsInstanceModel(bigraph, System.out);
        BigraphFileModelManagement.Store.exportAsMetaModel(bigraph, System.out);
        eb(bigraph, "agent", TARGET_DUMP_PATH2, true);

        DemoRuleProvider ruleProvider = DemoRuleProvider.getInstance();
        ReactionRule<PureBigraph> rr = ruleProvider.smartHomeMoveRule(sig);
//        ReactionRule<PureBigraph> rr = ruleProvider.petriNetParallelRule(sig);
        BigraphFileModelManagement.Store.exportAsInstanceModel(rr.getRedex(), System.out);
        BigraphFileModelManagement.Store.exportAsInstanceModel(rr.getReactum(), System.out);
        eb(rr.getRedex(), "redex", TARGET_DUMP_PATH2, true);
        eb(rr.getReactum(), "reactum", TARGET_DUMP_PATH2, true);

        PureReactiveSystem reactiveSystem = new PureReactiveSystem();
        reactiveSystem.setAgent(bigraph);
        reactiveSystem.addReactionRule(rr);

        PureBigraphModelChecker modelChecker = new PureBigraphModelChecker(
                reactiveSystem,
                BigraphModelChecker.SimulationStrategy.Type.BFS,
                opts(TARGET_DUMP_PATH2));
//        modelChecker.setReactiveSystemListener(this);
        modelChecker.execute();
    }

    private ModelCheckingOptions opts(String TARGET_DUMP_PATH) {
        Path completePath = Paths.get(TARGET_DUMP_PATH, "transition_graph.png");
        ModelCheckingOptions opts = ModelCheckingOptions.create();
        opts
                .and(transitionOpts()
                        .setMaximumTransitions(10)
                        .setMaximumTime(60)
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

}
