package io.github.spigotcvn.merger.mappings.files;

import java.io.*;

public interface Saveable {
    /**
     * Saves the class to an output stream
     * This could be a FileOutputStream, but if you need to save something
     * without a file, you can use a ByteArrayOutputStream or similar
     * @param os The output stream to save to
     * @throws IllegalStateException If an error occurs while writing to the stream
     */
    void saveToStream(OutputStream os) throws IllegalStateException;

    /**
     * Saves the class to a file and catches the IOException
     * If you need to catch it again, catch it as an UncheckedIOException
     * @see #saveToStream(OutputStream)
     * @param file The file to save to
     */
    default void saveToFile(File file) {
        try(OutputStream os = new FileOutputStream(file)) {
            saveToStream(os);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
