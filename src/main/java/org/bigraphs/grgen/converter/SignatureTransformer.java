package org.bigraphs.grgen.converter;

import org.bigraphs.framework.core.Signature;

public abstract class SignatureTransformer extends TransformerSupport implements BaseTransformer<Signature<?>> {
    Signature<?> signature;

    @Override
    public abstract String toString(Signature<?> signature);

//    /**
//     * Redirects the result of an encoding to an output stream.
//     *
//     * @param signature    the reactive system being encoded
//     * @param outputStream the output stream where the result shall be written to
//     * @throws IOException because of the stream
//     */
//    @Override
//    public abstract void toOutputStream(Signature<?> signature, OutputStream outputStream) throws IOException;
}
