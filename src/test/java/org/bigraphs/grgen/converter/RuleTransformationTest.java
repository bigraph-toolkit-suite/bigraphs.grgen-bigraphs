package org.bigraphs.grgen.converter;

import org.apache.commons.io.FileUtils;
import org.bigraphs.framework.core.BigraphFileModelManagement;
import org.bigraphs.framework.core.exceptions.InvalidConnectionException;
import org.bigraphs.framework.core.exceptions.InvalidReactionRuleException;
import org.bigraphs.framework.core.exceptions.builder.TypeNotExistsException;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.signature.DynamicSignature;
import org.bigraphs.framework.core.reactivesystem.ReactionRule;
import org.bigraphs.framework.core.reactivesystem.TrackingMap;
import org.bigraphs.grgen.converter.demo.DemoRuleProvider;
import org.bigraphs.grgen.converter.demo.DemoSignatureProvider;
import org.bigraphs.grgen.converter.impl.PureParametrizedRuleTransformer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * This class tests the rule transformation capabilities.
 *
 * @author Dominik Grzelak
 */
public class RuleTransformationTest implements BigraphUnitTestSupport {

    private final static String TARGET_DUMP_PATH = "src/test/resources/dump/petrinet/";
    private final static String TARGET_DUMP_PATH2 = "src/test/resources/dump/smarthome/";
//
    @BeforeClass
    static void setUp() throws IOException {
        File dump = new File(TARGET_DUMP_PATH);
        dump.mkdirs();
        File dump2 = new File(TARGET_DUMP_PATH2);
        dump2.mkdirs();
        FileUtils.cleanDirectory(new File(TARGET_DUMP_PATH));
        FileUtils.cleanDirectory(new File(TARGET_DUMP_PATH2));
        new File(TARGET_DUMP_PATH + "states/").mkdir();
        new File(TARGET_DUMP_PATH2 + "states/").mkdir();
    }

    @Test
    public void testPetriNetRuleToGrGenRule() throws InvalidConnectionException, TypeNotExistsException, InvalidReactionRuleException, IOException {
        DemoSignatureProvider signatureProvider = DemoSignatureProvider.getInstance();
        DynamicSignature sig = signatureProvider.petriNet();
//        BigraphFileModelManagement.Store.exportAsInstanceModel(sig, System.out);
//        BigraphFileModelManagement.Store.exportAsMetaModel(sig, System.out);
        DemoRuleProvider ruleProvider = DemoRuleProvider.getInstance();
        ReactionRule<PureBigraph> rr = ruleProvider.petriNetFireRule(sig);
        System.out.println("---------------------");
        BigraphFileModelManagement.Store.exportAsInstanceModel(rr.getRedex(), System.out);
        System.out.println("---------------------");
        BigraphFileModelManagement.Store.exportAsInstanceModel(rr.getReactum(), System.out);
        System.out.println("---------------------");


        PureParametrizedRuleTransformer t = new PureParametrizedRuleTransformer();

        // Tracking map from reactum to redex elements (right to left)
        TrackingMap trackingMap = RuleTransformer.createMap();
        trackingMap.put("v0", "v0"); // left-place of transition in redex
        trackingMap.put("v3", "v1"); // left token of left-place in redex
        trackingMap.put("v1", "v2"); // transition in redex
        trackingMap.put("v2", "v3"); // right-place of transition in redex
        trackingMap.put("e0", "e0"); // edge from left-place to transition
        trackingMap.put("e1", "e1"); // edges from transition to right-place
        trackingMap.addLinkNames("e0", "e1"); // and possible outer names
        t.withMap(trackingMap);
        String result = t.toString(rr);
        System.out.println("---------------------");
        System.out.println(result);
        System.out.println("---------------------");
    }

    @Test
    public void testPetriNetRuleToGrGenRule_WithOuterNames() throws InvalidConnectionException, TypeNotExistsException, InvalidReactionRuleException, IOException {
        DemoSignatureProvider signatureProvider = DemoSignatureProvider.getInstance();
        DynamicSignature sig = signatureProvider.petriNet();
        DemoRuleProvider ruleProvider = DemoRuleProvider.getInstance();
        ReactionRule<PureBigraph> rr = ruleProvider.petriNetFireRule_withOuterNames(sig);
        System.out.println("---------------------");
        BigraphFileModelManagement.Store.exportAsInstanceModel(rr.getRedex(), System.out);
        System.out.println("---------------------");
        BigraphFileModelManagement.Store.exportAsInstanceModel(rr.getReactum(), System.out);
        System.out.println("---------------------");


        PureParametrizedRuleTransformer t = new PureParametrizedRuleTransformer();

        // Tracking map from reactum to redex elements (right to left)
        TrackingMap trackingMap = RuleTransformer.createMap();
        trackingMap.put("v0", "v0"); // left-place of transition in redex
        trackingMap.put("v3", "v1"); // left token of left-place in redex
        trackingMap.put("v1", "v2"); // transition in redex
        trackingMap.put("v2", "v3"); // right-place of transition in redex
        trackingMap.put("tmp", "tmp"); // edge from left-place to transition
        trackingMap.put("tmp2", "tmp2"); // edges from transition to right-place
        trackingMap.addLinkNames("tmp", "tmp2"); // and possible outer names
        t.withMap(trackingMap);
        String result = t.toString(rr);
        System.out.println("---------------------");
        System.out.println(result);
        System.out.println("---------------------");
    }

    @Test
    public void test_PetriNetRule_AddToken() throws InvalidConnectionException, TypeNotExistsException, InvalidReactionRuleException, IOException {
        DemoSignatureProvider signatureProvider = DemoSignatureProvider.getInstance();
        DynamicSignature sig = signatureProvider.petriNet();
        DemoRuleProvider ruleProvider = DemoRuleProvider.getInstance();
        ReactionRule<PureBigraph> rr = ruleProvider.petriNetAddRule(sig);
        System.out.println("---------------------");
        BigraphFileModelManagement.Store.exportAsInstanceModel(rr.getRedex(), System.out);
        System.out.println("---------------------");
        BigraphFileModelManagement.Store.exportAsInstanceModel(rr.getReactum(), System.out);
        System.out.println("---------------------");


        PureParametrizedRuleTransformer t = new PureParametrizedRuleTransformer();

        // Tracking map from reactum to redex elements (right to left)
        TrackingMap trackingMap = RuleTransformer.createMap();
        trackingMap.put("v0", "v0");
        trackingMap.put("v1", "");
        trackingMap.put("x", "x");
        trackingMap.addLinkNames("x"); // and possible outer names
        t.withMap(trackingMap);
        String result = t.toString(rr);
        System.out.println("---------------------");
        System.out.println(result);
        System.out.println("---------------------");
    }

    @Test
    public void test_smartHomeMoveRule() throws Exception {
        DemoSignatureProvider signatureProvider = DemoSignatureProvider.getInstance();
        DynamicSignature sig = signatureProvider.smartHome();
        DemoRuleProvider ruleProvider = DemoRuleProvider.getInstance();
        ReactionRule<PureBigraph> rr = ruleProvider.smartHomeMoveRule(sig);
        System.out.println("---------------------");
        BigraphFileModelManagement.Store.exportAsInstanceModel(rr.getRedex(), System.out);
        System.out.println("---------------------");
        BigraphFileModelManagement.Store.exportAsInstanceModel(rr.getReactum(), System.out);
        System.out.println("---------------------");
        eb(rr.getRedex(), "redex", TARGET_DUMP_PATH2);
        eb(rr.getReactum(), "reactum", TARGET_DUMP_PATH2);


        PureParametrizedRuleTransformer t = new PureParametrizedRuleTransformer();
        // Tracking map from reactum to redex elements (right to left)
        TrackingMap trackingMap = RuleTransformer.createMap();
        trackingMap.put("v0", "v0");
        trackingMap.put("v1", "v2");
        trackingMap.put("v2", "v1"); // edges from transition to right-place
        trackingMap.put("x", "x"); // edges from transition to right-place
        trackingMap.addLinkNames("x"); // and possible outer names
        t.withMap(trackingMap);
        String result = t.toString(rr);
        System.out.println("---------------------");
        System.out.println(result);
        System.out.println("---------------------");
    }
}
