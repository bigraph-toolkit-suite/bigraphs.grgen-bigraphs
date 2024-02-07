package org.bigraphs.grgen.converter;

import org.bigraphs.framework.core.Bigraph;
import org.bigraphs.framework.core.Signature;

public abstract class BigraphTransformer extends TransformerSupport implements BaseTransformer<Bigraph<Signature<?>>> {

    @Override
    public abstract String toString(Bigraph<Signature<?>> signature);
}
