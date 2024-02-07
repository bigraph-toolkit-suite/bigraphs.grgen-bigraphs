package org.bigraphs.grgen.converter;

import org.apache.commons.cli.*;
import org.bigraphs.framework.core.AbstractEcoreSignature;
import org.bigraphs.framework.core.BigraphFileModelManagement;
import org.bigraphs.framework.core.Control;
import org.bigraphs.framework.core.exceptions.InvalidConnectionException;
import org.bigraphs.framework.core.exceptions.InvalidReactionRuleException;
import org.bigraphs.framework.core.exceptions.operations.IncompatibleInterfaceException;
import org.bigraphs.framework.core.factory.BigraphFactory;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.core.impl.signature.DefaultDynamicSignature;
import org.bigraphs.framework.core.reactivesystem.ParametricReactionRule;
import org.bigraphs.framework.core.utils.BigraphUtil;
import org.bigraphs.grgen.converter.cli.FileOps;
import org.bigraphs.grgen.converter.cli.GrGenScriptFileBuilder;
import org.bigraphs.grgen.converter.cli.InvalidRuleSpec;
import org.bigraphs.grgen.converter.cli.RuleSpec;
import org.bigraphs.grgen.converter.impl.DynamicSignatureTransformer;
import org.bigraphs.grgen.converter.impl.PureBigraphTransformer;
import org.bigraphs.grgen.converter.impl.PureParametrizedRuleTransformer;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bigraphs.framework.core.reactivesystem.TrackingMap;
/**
 * Main entry point for the command-line interface.
 *
 * @author Dominik Grzelak
 */
public class Main {
    static boolean verbose = false;

    // Some possible arguments for testing:
    // --host=bla --output=foo --rule=rule1:a,a --rule=:path,path
    // --host=bla --output=foo --rule=rule1:lhs,rhs --rule=:foobar
    // --host=bla --output=foo --rule=rule1:sample/petrinet-simple/r1-lhs.xmi,sample/petrinet-simple/r1-rhs.xmi --rule=:path,path
    // --host=bla --output=foo --rule=rule1:sample/petrinet-simple/r1-lhs.xmi,sample/petrinet-simple/r1-rhs.xmi --rule=:sample/petrinet-simple/r1-lhs.xmi,sample/petrinet-simple/r1-rhs.xmi
    // --verbose --host=sample/petrinet-simple/host.xmi --output=foo --sig=sample/petrinet-simple/sig.xmi --sigM=sample/petrinet-simple/signatureBaseModel.ecore --metamodel=sample/petrinet-simple/bigraphBaseModel.ecore --rule=rule1:sample/petrinet-simple/r1-lhs.xmi,sample/petrinet-simple/r1-rhs.xmi --rule=:sample/petrinet-simple/r1-lhs.xmi,sample/petrinet-simple/r1-rhs.xmi
    // --verbose --host=sample/petrinet-simple/host.xmi --output=foo --sig=sample/petrinet-simple/sig.xmi --sigM=sample/petrinet-simple/signatureBaseModel.ecore --metamodel=sample/petrinet-simple/bigraphBaseModel.ecore --rule=rule1:sample/petrinet-simple/r1-lhs.xmi,sample/petrinet-simple/r1-rhs.xmi --tracking=sample/petrinet-simple/map.json
    public static void main(String[] args) throws InvalidConnectionException, InvalidReactionRuleException, IncompatibleInterfaceException, IOException {
        String DEFAULT_VERSION = "(DRAFT)";
        String version = DEFAULT_VERSION;
        try (InputStream is = Main.class.getResourceAsStream("/META-INF/maven/org.example/grgen-bigraphs/pom.properties")) {
            Properties properties = new Properties();
            properties.load(is);
            version = properties.getProperty("version");
            if(version == null || version.isEmpty()) {
                version = DEFAULT_VERSION;
            }
        } catch (Exception e) {
            version = DEFAULT_VERSION;
        }
        String APP_NAME = "biGGer " + version;

        // Define command-line options
        Options options = new Options();

        Option signatureOpt = new Option("s", "sig", true, "Signature file path (*.xmi)");
        signatureOpt.setRequired(true);
        options.addOption(signatureOpt);

        Option signatureMMOpt = new Option("sm", "sigM", true, "Signature metamodel file path (*.ecore)");
        signatureOpt.setRequired(true);
        options.addOption(signatureMMOpt);

        Option input = new Option("i", "host", true, "Host bigraph file path (*.xmi)");
        input.setRequired(true);
        options.addOption(input);

        Option metamodel = new Option("m", "metamodel", true, "Metamodel file path (*.ecore)");
        metamodel.setRequired(false);
        options.addOption(metamodel);

        Option rulesOpt = new Option("r", "rule", true, "Rule file path. Format of the argument: <RULENAME>:<REDEX_FILE_XMI>,<REACTUM_FILE_XMI>");
        rulesOpt.setRequired(false);
        options.addOption(rulesOpt);

        Option tracking = new Option("t", "tracking", true, "Tracking map for each rule in the system (in JSON).");
        tracking.setRequired(false);
        options.addOption(tracking);

        Option output = new Option("o", "output", true, "Output directory path ");
        output.setRequired(false);
        options.addOption(output);

        Option basePathOpt = new Option("b", "basepath", true, "The base path to use for all other options.");
        basePathOpt.setRequired(false);
        options.addOption(basePathOpt);

        Option verbosity = new Option("v", "verbose", false, "Verbose output");
        options.addOption(verbosity);

        Option help = new Option("h", "help", false, "Display help");
        options.addOption(help);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                formatter.printHelp(APP_NAME, options);
                System.exit(0);
            }

            if (cmd.hasOption("verbose")) {
                verbose = true;
            }

            String baseFilePath = cmd.getOptionValue("basepath", "");
            String inputFilePath = cmd.getOptionValue("host");
            String outputFilePath = cmd.getOptionValue("output");
            String trackingMapFilePath = cmd.getOptionValue("tracking");
            String metamodelFilePath = cmd.getOptionValue("metamodel");
            String signatureMetamodelFilePath = cmd.getOptionValue("sigM");
            String signatureFilePath = cmd.getOptionValue("sig");
            String[] ruleValues = cmd.getOptionValues(rulesOpt);
            if (ruleValues == null) ruleValues = new String[]{};

            // Verbose = ON?
            verbose(() -> System.out.println("Verbosity: " + verbose));
            verbose(() -> System.out.println("Base file path: " + baseFilePath));
            verbose(() -> System.out.println("Output file: " + outputFilePath));
            verbose(() -> System.out.println("Signature file: " + signatureFilePath));
            verbose(() -> System.out.println("Signature metamodel file: " + signatureMetamodelFilePath));
            verbose(() -> System.out.println("Host graph file: " + inputFilePath));
            verbose(() -> System.out.println("Host graph metamodel file: " + metamodelFilePath));
            String[] finalRuleValues = ruleValues;
            verbose(() -> System.out.println("Rules: " + Arrays.toString(finalRuleValues)));
            verbose(() -> System.out.println("Tracking map file: " + trackingMapFilePath));
            verbose(System.out::println);

            // Check if the rule format is correct and that all files exist
            // If there is an invalid spec, report the error and exit the program
            final List<RuleSpec> collect = Arrays.stream(ruleValues).map(x -> {
                if (checkRuleFormat(x)) {
                    String[] entries = getRuleFormatEntries(x);
                    return new RuleSpec(entries[0], entries[1], entries[2]);
                } else {
                    return new InvalidRuleSpec(x, "", "");
                }
            }).collect(Collectors.toList());
            List<RuleSpec> invalidSpecs = collect.stream().filter(x -> x instanceof InvalidRuleSpec).collect(Collectors.toList());
            if (!invalidSpecs.isEmpty()) {
                System.out.println("Some of the rule specifications are invalid:");
                invalidSpecs.forEach(x -> {
                    int ruleIx = collect.indexOf(x) + 1;
                    String s = x.getRuleName();
                    if(s.isEmpty()) s = x.getRedexFilePath();
                    if(s.isEmpty()) s = x.getReactumFilePath();
                    if(s.isEmpty()) s = x.toString();
                    System.out.println("\tRule at position = " + ruleIx + ": " + s);
                });
                System.exit(1);
            }
            invalidSpecs = collect.stream().filter(x -> !FileOps.fileExists(baseFilePath, x.getRedexFilePath()) ||
                            !FileOps.fileExists(baseFilePath, x.getReactumFilePath()))
                    .collect(Collectors.toList());
            if (!invalidSpecs.isEmpty()) {
                System.out.println("Some of the rule specifications have invalid files specified:");
                invalidSpecs.forEach(x -> {
                    int ruleIx = collect.indexOf(x) + 1;
                    String s = x.getRuleName();
                    if(s.isEmpty()) s = x.getRedexFilePath();
                    if(s.isEmpty()) s = x.getReactumFilePath();
                    if(s.isEmpty()) s = x.toString();
                    System.out.println("\tRule at position = " + ruleIx + ": " + s);
                });
                System.exit(1);
            }


            // "Repair" the rule names: if none is set, generate one
            Supplier<String> ruleNameSupp = new Supplier<>() {
                private int id = 0;

                @Override
                public String get() {
                    return "rule" + id++;
                }
            };
            List<RuleSpec> collectRepaired = collect.stream().map(x -> {
                if (x.getRuleName().isEmpty()) {
                    int ruleIx = collect.indexOf(x) + 1;
                    System.out.println("Labeled unnamed rule at position = " + ruleIx);
                    x.setRuleName(ruleNameSupp.get());
                    System.out.println("\tNew label is = " + x.getRuleName());
                }
                return x;
            }).collect(Collectors.toList());
            List<String> availableRuleNames = collectRepaired.stream().map(RuleSpec::getRuleName).collect(Collectors.toList());
            System.out.println("Rules recognized successfully (" + collectRepaired.size() + "): " + availableRuleNames);

            // Check signature if provided
            if (!FileOps.fileExists(baseFilePath, signatureFilePath)) {
                System.out.println("The signature file does not exist: " + signatureFilePath);
                System.exit(1);
            } else {
                verbose(() -> System.out.println("Signature recognized: " + signatureFilePath));
            }
            if (!FileOps.fileExists(baseFilePath, signatureMetamodelFilePath)) {
                System.out.println("The signature metamodel file does not exist: " + signatureMetamodelFilePath);
                System.exit(1);
            } else {
                verbose(() -> System.out.println("Signature metamodel recognized: " + signatureMetamodelFilePath));
            }

            // Check bigraph metamodel and instance (host graph input) if provided
            if (!FileOps.fileExists(baseFilePath, metamodelFilePath)) {
                System.out.println("The metamodel file does not exist: " + metamodelFilePath);
                System.exit(1);
            } else {
                verbose(() -> System.out.println("Metamodel recognized: " + metamodelFilePath));
            }
            if (!FileOps.fileExists(baseFilePath, inputFilePath)) {
                System.out.println("The host bigraph input file does not exist: " + inputFilePath);
                System.exit(1);
            } else {
                verbose(() -> System.out.println("Host bigraph recognized: " + inputFilePath));
            }

            // Check output file path and create if necessary
            if (!FileOps.directoryExists(baseFilePath, outputFilePath)) {
                verbose(() -> System.out.println("Output file path does not exist and will be created now: " + outputFilePath));
                File dump = new File(Paths.get(baseFilePath, outputFilePath).toAbsolutePath().toString());
                dump.mkdirs();
            } else {
                verbose(() -> System.out.println("Output file path exists: " + outputFilePath));
            }

            // Check output file path and create if necessary
            if (!FileOps.fileExists(baseFilePath, trackingMapFilePath)) {
                verbose(() -> System.out.println("Tracking map file path does not exist: " + trackingMapFilePath));
                System.exit(1);
            } else {
                verbose(() -> System.out.println("Tracking map file path found: " + trackingMapFilePath));
            }

            Set<TrackingMap> tmaps = new LinkedHashSet<>();
            boolean noRulesSpecified = ruleValues.length == 0 ? true : false;
            if (!noRulesSpecified) {
                assert invalidSpecs.isEmpty();
                assert collectRepaired.isEmpty();
                // Tracking Map
                tmaps = TrackingMap.read(Paths.get(baseFilePath, trackingMapFilePath).toAbsolutePath().toString());
                if (ruleValues.length > tmaps.size()) {
                    throw new RuntimeException(
                            String.format(
                                    "Tracking map might be incomplete. There were more rules (%d) specified than in the tracking map (%d). Please check that every rule has a tracking map definition.",
                                    ruleValues.length,
                                    tmaps.size()
                            )
                    );
                }
                Set<String> ruleNamesDistinct = Arrays.stream(ruleValues).map(x -> x.split(":")[0]).collect(Collectors.toSet());
                HashSet<String> set = tmaps.stream().map(TrackingMap::getRuleName).collect(Collectors.toCollection(HashSet::new));
                boolean allRulesHaveTrackingMap = set.containsAll(ruleNamesDistinct);
                if (!allRulesHaveTrackingMap) {
                    throw new RuntimeException(
                            String.format(
                                    "Not all rulesOpt specified via %s have a corresponding entry in the supplied tracking map (%s). Please check.",
                                    rulesOpt.getLongOpt(),
                                    trackingMapFilePath
                            )
                    );
                }
            } else {
                verbose(() -> System.out.println("Tracking map will not be evaluated because no rules were specified."));
            }

            System.out.println("Loading now: " + Paths.get(baseFilePath, signatureMetamodelFilePath).toRealPath().toAbsolutePath());
            System.out.println("Loading now: " + Paths.get(baseFilePath, signatureFilePath).toRealPath().toAbsolutePath());
            // Load all given files (signatures, bigraphs and rulesOpt as well as the metamodels)
            List<EObject> eObjects = BigraphFileModelManagement.Load.signatureInstanceModel(
                    Paths.get(baseFilePath, signatureMetamodelFilePath).toAbsolutePath().toString(),
                    Paths.get(baseFilePath, signatureFilePath).toAbsolutePath().toString()
            );
            EObject sigInstance = eObjects.get(0);
            AbstractEcoreSignature<? extends Control<?, ?>> sig = BigraphFactory.createOrGetSignature(sigInstance);
//            EPackage orGetBigraphMetaModel = BigraphFactory.createOrGetBigraphMetaModel(sig);
            System.out.println("Signature loaded successfully.");
            System.out.println("Loading now: " + Paths.get(baseFilePath, metamodelFilePath).toRealPath().toAbsolutePath());
            System.out.println("Loading now: " + Paths.get(baseFilePath, inputFilePath).toRealPath().toAbsolutePath());
//            EPackage ePackage = BigraphFileModelManagement.Load.bigraphMetaModel(Paths.get(baseFilePath, metamodelFilePath).toAbsolutePath().toString());
            EPackage ePackage = BigraphFileModelManagement.Load.bigraphMetaModel(Paths.get(baseFilePath, metamodelFilePath).toAbsolutePath().toString(), false);
            System.out.println("Bigraph metamodel loaded successfully.");
            List<EObject> eObjects1 = BigraphFileModelManagement.Load.bigraphInstanceModel(
                    ePackage,
//                    Paths.get(baseFilePath, metamodelFilePath).toAbsolutePath().toString(),
                    Paths.get(baseFilePath, inputFilePath).toAbsolutePath().toString()
            );
            EObject bigraphInstance = eObjects1.get(0);
            PureBigraph hostBigraph = BigraphUtil.toBigraph(ePackage, bigraphInstance, (DefaultDynamicSignature) sig);
//            PureBigraph hostBigraph = PureBigraphBuilder.create((DefaultDynamicSignature) sig, ePackage, bigraphInstance).createBigraph();
//            PureBigraph hostBigraph = BigraphUtil.toBigraph(orGetBigraphMetaModel, bigraphInstance, (DefaultDynamicSignature) sig);
            System.out.println("Host bigraph loaded successfully.");

            String primeMetamodelFileName = "metamodel_graph.gm";
            String primeHostmodelFileName = "host_graph.grs";
            // Export everything
            SignatureTransformer signatureTransformer = new DynamicSignatureTransformer();
            String signatureStr = signatureTransformer.toString(sig);
//            System.out.println(signatureStr);
            FileOps.writeFile(signatureStr, baseFilePath, Paths.get(outputFilePath, primeMetamodelFileName).toString());
            verbose(() -> System.out.println("Metamodel graph file written (" + primeMetamodelFileName + ")"));

            PureBigraphTransformer transformer = new PureBigraphTransformer();
            transformer.withOppositeEdges(false);
            String grgenGraphModel = transformer.toString(hostBigraph);
//            System.out.println(grgenGraphModel);
            grgenGraphModel = String.format(
                    "new graph ruleset \"Graph\"%s%s", System.getProperty("line.separator") + System.getProperty("line.separator"),
                    grgenGraphModel);
            FileOps.writeFile(grgenGraphModel, baseFilePath, Paths.get(outputFilePath, primeHostmodelFileName).toString());
            verbose(() -> System.out.println("Host graph file written (" + primeHostmodelFileName + ")"));

            if (!noRulesSpecified) {
                StringBuilder ruleBuilder = new StringBuilder();
                boolean generateNACPatternTemplate = true;
                for (RuleSpec eachRuleSpec : collectRepaired) {
                    RuleTransformer ruleTransformer = new PureParametrizedRuleTransformer()
                            .printNACPattern(generateNACPatternTemplate);
                    generateNACPatternTemplate = false; // only once for all rules
//                    if (generateNACPatternTemplate) {
//                        ruleTransformer.printNACPattern(false);
//                        generateNACPatternTemplate = false;
//                    }
                    Optional<TrackingMap> first = tmaps.stream().filter(x -> x.getRuleName().equals(eachRuleSpec.getRuleName())).findFirst();
                    if (first.isEmpty()) {
                        throw new RuntimeException(
                                String.format("Tracking map for rule %s could not be found in %s",
                                        eachRuleSpec.getRuleName(),
                                        trackingMapFilePath
                                )
                        );
                    }
                    ruleTransformer.withMap(first.get());
                    List<EObject> eObjectsRedex = BigraphFileModelManagement.Load.bigraphInstanceModel(
                            ePackage,
                            Paths.get(baseFilePath, eachRuleSpec.getRedexFilePath()).toAbsolutePath().toString()
                    );
                    List<EObject> eObjectsReactum = BigraphFileModelManagement.Load.bigraphInstanceModel(
                            ePackage,
                            Paths.get(baseFilePath, eachRuleSpec.getReactumFilePath()).toAbsolutePath().toString()
                    );
//                PureBigraph hostBigraph = PureBigraphBuilder.create((DefaultDynamicSignature) sig, ePackage, bigraphInstance).createBigraph();
                    PureBigraph redex = BigraphUtil.toBigraph(ePackage, eObjectsRedex.get(0), (DefaultDynamicSignature) sig);
                    PureBigraph reactum = BigraphUtil.toBigraph(ePackage, eObjectsReactum.get(0), (DefaultDynamicSignature) sig);
                    String ruleEncoded = ruleTransformer.toString(new ParametricReactionRule<>(redex, reactum).withLabel(eachRuleSpec.getRuleName()));
                    ruleBuilder.append(ruleEncoded);
                }

                ruleBuilder.insert(0, String.format("#using \"%s\"%s", primeMetamodelFileName, System.getProperty("line.separator")));
                FileOps.writeFile(ruleBuilder.toString(), baseFilePath, Paths.get(outputFilePath, "ruleset.grg").toString());
                verbose(() -> System.out.println("Ruleset file written (ruleset.grg)"));
            }

            GrGenScriptFileBuilder scriptFileBuilder = new GrGenScriptFileBuilder("");
            String scriptFileGenerated = scriptFileBuilder.generateScriptFile(primeHostmodelFileName, availableRuleNames);
            FileOps.writeFile(scriptFileGenerated, baseFilePath, Paths.get(outputFilePath, "script.grs").toString());
            verbose(() -> System.out.println("Script file written (script.grs)"));


            System.out.println("Conversion finished successfully. All model files are created in the folder " +
                    Paths.get(baseFilePath, outputFilePath).toRealPath().toAbsolutePath());

        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            formatter.printHelp(APP_NAME, options);
            System.exit(1);
        }
    }

    public static void verbose(Runnable r) {
        if (verbose)
            r.run();
    }

    public static boolean checkRuleFormat(String singleRuleSpec) {
        // "^([^:]+)?:[^,]+,[^,]+$" -> optional rule name
        // "^[^:]+:[^,]+,[^,]+$" -> mandatory rule name
        String regex = "^([^:]+)?:[^,]+,[^,]+$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(singleRuleSpec);

        if (matcher.matches()) {
            verbose(() -> System.out.println("Rule specification conforms to the format."));
            return true;
        } else {
            verbose(() -> System.out.println("Rule specification does not conform to the format."));
            return false;
        }
    }

    public static String[] getRuleFormatEntries(String singleRuleSpec) {
        // Both work
//        String regex = "^([^:]+)?:([^,]+),([^,]+)$";
        String regex = "(?m)^(.*?)?:(.*?),(.*?)$";
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(singleRuleSpec);
        if (matcher.matches()) {
            String ruleName = matcher.group(1);         // Extract <RULENAME>
            String redexFileXMI = matcher.group(2);     // Extract <REDEX_FILE_XMI>
            String reactumFileXMI = matcher.group(3);   // Extract <REACTUM_FILE_XMI>

            verbose(() -> System.out.println("\tRULE_NAME: " + ruleName));
            verbose(() -> System.out.println("\tREDEX_FILE_XMI: " + redexFileXMI));
            verbose(() -> System.out.println("\tREACTUM_FILE_XMI: " + reactumFileXMI));
            verbose(System.out::println);
            return new String[]{ruleName, redexFileXMI, reactumFileXMI};
        } else {
            verbose(() -> System.out.println("A rule option does not conform to the format."));
            return new String[]{"", "", ""};
        }
    }
}
