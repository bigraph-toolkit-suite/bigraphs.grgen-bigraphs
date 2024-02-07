package org.bigraphs.grgen.converter.cli;

public class RuleSpec {
    private String ruleName = "";
    private String redexFilePath = "";
    private String reactumFilePath = "";

    public RuleSpec(String ruleName, String redexFilePath, String reactumFilePath) {
        this.ruleName = ruleName == null ? "" : ruleName;
        this.redexFilePath = redexFilePath == null ? "" : redexFilePath;
        this.reactumFilePath = reactumFilePath == null ? "" : reactumFilePath;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRedexFilePath() {
        return redexFilePath;
    }

    public void setRedexFilePath(String redexFilePath) {
        this.redexFilePath = redexFilePath;
    }

    public String getReactumFilePath() {
        return reactumFilePath;
    }

    public void setReactumFilePath(String reactumFilePath) {
        this.reactumFilePath = reactumFilePath;
    }
}
