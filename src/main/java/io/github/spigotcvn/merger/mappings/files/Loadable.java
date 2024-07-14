package io.github.spigotcvn.merger.mappings.files;

import java.io.*;

public interface Loadable {
    /**
     * Loads the class from an input stream
     * This could be a FileInputStream, but if you need to load something
     * without a file, you can use a ByteArrayInputStream or similar
     * @param is The input stream to load from
     * @throws IOException If an error occurs while reading the stream
     */
    void loadFromStream(InputStream is) throws IOException;

    /**
     * Loads the class from a file and catches the IOException
     * If you need to catch it again, catch it as an UncheckedIOException
     * @see #loadFromStream(InputStream)
     * @param file The file to load from
     */
    default void loadFromFile(File file) {
        try(InputStream os = new FileInputStream(file)) {
            loadFromStream(os);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
