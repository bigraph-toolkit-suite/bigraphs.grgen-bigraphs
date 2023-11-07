package org.example;

import org.bigraphs.framework.core.Control;
import org.bigraphs.framework.core.exceptions.BigraphIsNotGroundException;
import org.bigraphs.framework.core.exceptions.BigraphIsNotPrimeException;
import org.bigraphs.framework.core.impl.pure.PureBigraph;

/**
 * Abstract support class that provides support to other transformer-related classes
 * It cannot be instantiated directly.
 * <p>
 * It is designed to be extended by direct implementations of {@link org.example.BaseTransformer}, e.g., {@link org.example.BigraphTransformer},
 * to provide support to their common subclasses.
 *
 * @author Dominik Grzelak
 */
public abstract class TransformerSupport {

    protected String LINE_SEP = System.getProperty("line.separator");
    protected char DEFAULT_WHITESPACE_CHAR = ' ';

    public void assertCorrectClassType(Class classType, Object instance) throws RuntimeException {
        if (!instance.getClass().isAssignableFrom(classType)) {
            throw new RuntimeException("The transformer " + this.getClass() + "cannot be applied on class " + instance.getClass());
        }
    }

    public void assertPrimeAndGround(PureBigraph bigraph) throws RuntimeException {
        if (!bigraph.isGround()) {
            throw new BigraphIsNotGroundException();
        }
        if (!bigraph.isPrime()) {
            throw new BigraphIsNotPrimeException();
        }
    }

    // Metamodel Builders

    protected String createAbstractNodeClass(String abstractNodeClassIdentifier) {
        return String.format("abstract node class %s", abstractNodeClassIdentifier);
    }

    protected String createAbstractEdgeClass(String abstractEdgeClassIdentifier) {
        return String.format("abstract edge class %s", abstractEdgeClassIdentifier);
    }

    protected String createNodeClass(String nodeClassIdentifier) {
        return String.format("node class %s", nodeClassIdentifier);
    }

    protected String createEdgeClass(String edgeClassIdentifier) {
        return String.format("edge class %s", edgeClassIdentifier);
    }

    protected String extendClass(String classPrefix, String superClassIdentifier) {
        return String.format("%s extends %s", classPrefix, superClassIdentifier);
    }

    protected String closeClassDefinition(String classPrefix) {
        return String.format("%s { }", classPrefix);
    }

    protected String closeClassDefinitionWithAttributes(String classPrefix, String attributeList) {
        if (attributeList == null || attributeList.isEmpty())
            return String.format("%s { }", classPrefix);
        return String.format("%s {\r\n%s\r\n}", classPrefix, attributeList);
    }

    // Model Graph Builders

    protected String createNodeInstance(String classType, String name, String variable) {
        return String.format("new %s:%s($=%s)", variable, classType, name);
    }

    protected String createNodeInstance(String classType, String name, String variable, int index) {
        return String.format("new %s:%s($=%s, ix=%d)", variable, classType, name, index);
    }

    protected String createEdge(String edgeClassType, String edgeVar, String nodeVariableLeft, String nodeVariableRight) {
        return String.format(
                "new %s -%s:%s-> %s",
                nodeVariableLeft,
                edgeVar, edgeClassType,
                nodeVariableRight
        );
    }

    /////////////////
    // Rule Builders
    /////////////////

    /**
     * Creates the variable {@code varName:type}
     *
     * @param varName
     * @param type
     * @return the variable declaration
     */
    protected String createVarTypeDecl(String varName, String type) {
        return String.format(
                "%s:%s",
                varName,
                type
        );
    }

    /**
     * Creates the variable {@code varName:nodeType;}
     *
     * @param varName
     * @param nodeType
     * @return the variable declaration
     */
    protected String createVarTypeDeclWithComma(String varName, String nodeType) {
        return String.format("%s:%s;", varName, nodeType);
    }

    protected String createVarTypeDeclWithComma(String varName, Control nodeTypeAsControl) {
        return String.format("%s:%s;", varName, nodeTypeOf(nodeTypeAsControl));
    }

    protected String nodeTypeOf(Control control) {
        return String.format("%s", control.getNamedType().stringValue());
    }

    /**
     * Creates the graphlet {@code leftPart -middlePart-> rightPart;}
     *
     * @param leftPart
     * @param middlePart
     * @param rightPart
     * @return the graphlet
     */
    protected String createGraphlet(String leftPart, String middlePart, String rightPart) {
        return String.format(
                "%s -%s-> %s;",
                leftPart,
                middlePart,
                rightPart
        );
    }

    protected String openRuleModifierBlock() {
        return "modify {";
    }

    protected String openRuleReplaceBlock() {
        return "replace {";
    }

    protected String closeBlock() {
        return LINE_SEP + "}";
    }

}
