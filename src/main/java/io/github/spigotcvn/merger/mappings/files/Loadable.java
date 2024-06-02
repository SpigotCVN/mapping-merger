package io.github.spigotcvn.merger.mappings.files;

import io.github.spigotcvn.merger.mappings.InvalidMappingFormatException;

import java.io.*;

public interface Loadable {
    void loadFromStream(InputStream is) throws InvalidMappingFormatException;

    default void loadFromFile(File file) throws InvalidMappingFormatException {
        try(InputStream os = new FileInputStream(file)) {
            loadFromStream(os);
        } catch (IOException e) {
            throw new IllegalArgumentException("File not found: " + file.getAbsolutePath());
        }
    }
}
