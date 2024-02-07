package org.bigraphs.grgen.converter;

import org.bigraphs.framework.core.Bigraph;
import org.bigraphs.framework.core.BigraphFileModelManagement;
import org.bigraphs.framework.core.impl.pure.PureBigraph;
import org.bigraphs.framework.visualization.BigraphGraphvizExporter;

import java.io.File;
import java.io.IOException;

/**
 * Support interface for all unit tests.
 * Provides convenience methods.
 *
 * @author Dominik Grzelak
 */
public interface BigraphUnitTestSupport {

    default void eb(Bigraph<?> bigraph, String name, String basePath) {
        eb(bigraph, name, basePath, true);
    }

    default void eb(Bigraph<?> bigraph, String name, String basePath, boolean asTree) {
        try {
            BigraphGraphvizExporter.toPNG(bigraph, asTree, new File(basePath + name + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    default void print(PureBigraph bigraph) {
        try {
            BigraphFileModelManagement.Store.exportAsInstanceModel(bigraph, System.out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    default void printMetaModel(PureBigraph bigraph) {
        try {
            BigraphFileModelManagement.Store.exportAsMetaModel(bigraph, System.out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
