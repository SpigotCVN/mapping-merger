package io.github.spigotcvn.merger.mappings.files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public interface Saveable {
    void saveToStream(OutputStream os) throws IllegalStateException;

    default void saveToFile(File file) {
        try(OutputStream os = new FileOutputStream(file)) {
            saveToStream(os);
        } catch (IOException e) {
            throw new IllegalArgumentException("File not found: " + file.getAbsolutePath());
        }
    }
}
