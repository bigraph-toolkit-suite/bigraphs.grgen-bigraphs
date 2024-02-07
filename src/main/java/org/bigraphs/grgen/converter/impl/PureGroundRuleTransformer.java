package org.bigraphs.grgen.converter.impl;

import org.bigraphs.framework.core.reactivesystem.ReactionRule;
import org.bigraphs.grgen.converter.RuleTransformer;

/**
 * A concrete transformer implementation for ground rules that take pure bigraphs as redex and reactum.
 * A rule is ground if it has no sites and inner names.
 *
 * @author Dominik Grzelak
 */
public class PureGroundRuleTransformer extends RuleTransformer {

    @Override
    public String toString(ReactionRule<?> element) {

        // In every node add a NAC that checks that there are no other nodes than the one specified.
        // use a size()/etc. function of GrGen maybe

        return null;
    }
}
