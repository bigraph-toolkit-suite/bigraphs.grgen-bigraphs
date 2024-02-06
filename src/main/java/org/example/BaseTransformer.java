package org.example;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Supplier;

/**
 * Base Model-to-Text transformation interface for all kinds f bigraphs, rules, and signatures.
 *
 * @param <V> bigraph or signature class type
 */
public interface BaseTransformer<V> {

    /**
     * Redirects the result of an encoding to a string
     *
     * @param element the bigraph or signature being encoded
     */
    String toString(V element);

    /**
     * Redirects the result of an encoding to an output stream.
     *
     * @param element      the bigraph or signature being encoded
     * @param outputStream the output stream where the result shall be written to
     * @throws IOException because of the stream
     */
    default void toOutputStream(V element, OutputStream outputStream) throws IOException {
        String s = toString(element);
        outputStream.write(s.getBytes(), 0, s.length());
    }

    default Supplier<String> createNameSupplier(final String prefix) {
        return new Supplier<String>() {
            private int id = 0;

            @Override
            public String get() {
                return prefix + id++;
            }
        };
    }
}
