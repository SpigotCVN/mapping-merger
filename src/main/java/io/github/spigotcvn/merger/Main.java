package io.github.spigotcvn.merger;

import io.github.spigotcvn.merger.mappings.InvalidMappingFormatException;
import io.github.spigotcvn.merger.mappings.files.CSRGMappingFile;
import io.github.spigotcvn.merger.mappings.files.TinyMappingFile;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.*;

public class Main {
    public static void main(String[] args) throws InvalidMappingFormatException {
        OptionParser parser = new OptionParser();
        parser.accepts("from").withRequiredArg().ofType(File.class).required()
                .describedAs("The csrg mapping that contains the mappings you want to merge into the to mapping.");
        parser.accepts("to").withRequiredArg().ofType(File.class).required()
                .describedAs("The tiny mapping that you want to merge the from mapping into.");
        parser.accepts("out").withRequiredArg().ofType(File.class).required()
                .describedAs("The file to save the merged mappings to.");
        parser.accepts("namespace").withRequiredArg().ofType(String.class).required()
                .describedAs("The namespace to use for the merged mappings.");

        OptionSet options = parser.parse(args);
        File from = (File) options.valueOf("from");
        File to = (File) options.valueOf("to");
        File out = (File) options.valueOf("out");
        String namespace = (String) options.valueOf("namespace");

        CSRGMappingFile fromFile = new CSRGMappingFile();
        fromFile.loadFromFile(from);
        TinyMappingFile toFile = new TinyMappingFile();
        toFile.loadFromFile(to);

        System.out.println("Merging mappings " + from.getName() + " into " + to.getName() + ".");
        System.out.println("The process may take a while, please wait...");

        long start = System.currentTimeMillis();
        long startMerge = System.currentTimeMillis();
        MappingMerger.mergeTinyWithCSRG(toFile, fromFile, namespace);
        long endMerge = System.currentTimeMillis();
        System.out.println("Merging mappings took " + (endMerge - startMerge) + "ms.");
        System.out.println("Saving mappings to " + out.getName() + ".");
        System.out.println("The process may take a while, please wait...");

        long startSave = System.currentTimeMillis();
        toFile.saveToFile(out);
        long endSave = System.currentTimeMillis();
        System.out.println("Saving mappings took " + (endSave - startSave) + "ms.");
        long end = System.currentTimeMillis();
        System.out.println("The whole process took " + (end - start) + "ms.");
    }
}
