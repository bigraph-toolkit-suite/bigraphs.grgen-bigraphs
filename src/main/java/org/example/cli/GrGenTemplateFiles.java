package org.example.cli;

public abstract class GrGenTemplateFiles {

    public final static String SCRIPT_FILE = "include \"%s.grs\"\r\n" +
            "show graph ycomp\r\n" +
            "save graph \"graph_before.grs\"\r\n" +
            "exec %s\r\n" +
            "show graph ycomp\r\n" +
            "save graph \"graph_after.grs\"";
}
