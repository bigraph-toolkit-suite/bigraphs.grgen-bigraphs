package org.example.cli;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * A library class offering useful file operations on the filesystem via static methods.
 *
 * @author Dominik Grzelak
 */
public class FileOps {

    public static boolean fileExists(String filename) {
        if (filename == null) return false;
        File file = new File(filename);
        return file.isFile() && !file.isDirectory() && file.exists();
    }

    public static boolean fileExists(String basePath, String filename) {
        return fileExists(basePath + filename);
    }

    public static boolean directoryExists(String filename) {
        if (filename == null) return false;
        File file = new File(filename);
        return !file.isFile() && file.isDirectory() && file.exists();
    }

    public static boolean directoryExists(String basePath, String filename) {
        return directoryExists(basePath + filename);
    }

    public static void writeFile(String contents, String filePath) throws IOException {
        IOUtils.write(contents, new FileOutputStream(filePath), StandardCharsets.UTF_8);
    }

    public static void writeFile(String contents, String basePath, String filePath) throws IOException {
        writeFile(contents, basePath + filePath);
    }
}
