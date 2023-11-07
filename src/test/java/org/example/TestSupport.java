package org.example;

import org.bigraphs.framework.core.Bigraph;
import org.bigraphs.framework.visualization.BigraphGraphvizExporter;

import java.io.File;
import java.io.IOException;

public abstract class TestSupport {

    public void eb(Bigraph<?> bigraph, String name, String TARGET_DUMP_PATH) {
        this.eb(bigraph, name, TARGET_DUMP_PATH, true);
    }

    public void eb(Bigraph<?> bigraph, String name, String TARGET_DUMP_PATH, boolean asTree) {
        try {
            BigraphGraphvizExporter.toPNG(bigraph, asTree, new File(TARGET_DUMP_PATH + name + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
