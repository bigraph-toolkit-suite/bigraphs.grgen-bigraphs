package org.example.cli;

import java.util.List;

/**
 * This class creates a GrGen.NET script file with the file extension *.grs.
 * The script file includes the host graph (*.grs) and implements a simple rule execution strategy for testing purposes.
 * The output before and after the transformation is visualized via yComp.
 *
 * @author Dominik Grzelak
 */
public class GrGenScriptFileBuilder {

    private String projectName;

    public GrGenScriptFileBuilder(String projectName) {
        this.projectName = projectName;
    }

    /**
     * It generates a script template that can be executed with GrShell.
     * <p>
     * The employed rule execution strategy is simple.
     * Execute every rule (given as a list via the argument {@code ruleNames}) by a single GrGen.NET exec statement.
     * Only the first rule is actually executed, the other statements are commented out.
     *
     * @param hostGraphFileName the filename of the host graph (*.grs)
     * @param ruleNames         the rule names to execute
     * @return the script template
     */
    public String generateScriptFile(String hostGraphFileName, List<String> ruleNames) {
        StringBuilder sbRuleExec = new StringBuilder("");
        ruleNames.forEach(r -> {
            sbRuleExec.append(String.format(TEMPLATE_EXEC_COMMAND, r, 1));
        });

        return String.format(TEMPLATE_SCRIPT_FILE, hostGraphFileName, sbRuleExec);
    }

    // host graph filename
    // exec commands
    private final String TEMPLATE_SCRIPT_FILE = "" +
            "include \"%s\"\n" +
            "\n" +
            "debug set layout Organic\n" +
            "\n" +
            "show graph ycomp\n" +
            "\n" +
            "%s" +
            "\n" +
            "show graph ycomp\n";

    private final String TEMPLATE_EXEC_COMMAND = "exec %s[%d]\n"; // rule name, execution count
}
