package org.example.cli;

public class InvalidRuleSpec extends RuleSpec {

    boolean nameIsMissing = false;
    boolean redexIsMissing = false;
    boolean reactumIsMissing = false;

    public InvalidRuleSpec(String ruleName, String redexFilePath, String reactumFilePath) {
        super(ruleName, redexFilePath, reactumFilePath);
    }

    public InvalidRuleSpec() {
        super("", "", "");
        setNameIsMissing(true);
        setRedexIsMissing(true);
        setReactumIsMissing(true);
    }

//    public InvalidRuleSpec(String ruleName, String redexOrReactumFilePath, boolean redexOrReactum) {
//        this();
//        if (redexOrReactum) { // reactum is missing
//            setRedexIsMissing(false);
//            setRedexFilePath(redexOrReactumFilePath);
//        } else { // redex is missing
//            setReactumIsMissing(false);
//            setReactumFilePath(redexOrReactumFilePath);
//        }
//    }

    public boolean isNameIsMissing() {
        return nameIsMissing;
    }

    public void setNameIsMissing(boolean nameIsMissing) {
        this.nameIsMissing = nameIsMissing;
    }

    public boolean isRedexIsMissing() {
        return redexIsMissing;
    }

    public void setRedexIsMissing(boolean redexIsMissing) {
        this.redexIsMissing = redexIsMissing;
    }

    public boolean isReactumIsMissing() {
        return reactumIsMissing;
    }

    public void setReactumIsMissing(boolean reactumIsMissing) {
        this.reactumIsMissing = reactumIsMissing;
    }
}
