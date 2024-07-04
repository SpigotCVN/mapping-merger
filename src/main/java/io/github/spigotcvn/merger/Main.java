package io.github.spigotcvn.merger;

import io.github.spigotcvn.merger.mappings.InvalidMappingFormatException;
import io.github.spigotcvn.merger.mappings.files.CSRGMappingFile;
import io.github.spigotcvn.merger.mappings.files.TinyMappingFile;
import io.github.spigotcvn.merger.mappings.types.Mapping;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.*;

public class Main {
    public static void main(String[] args) throws InvalidMappingFormatException {
        OptionParser parser = new OptionParser();
        parser.accepts("mode").withRequiredArg().ofType(RunMode.class).required()
                .describedAs("The mode to run the program in. Either MERGE_TINY, REPLACE_ORIGINAL, CREATE_CSRG_FROM_TINY");
        parser.accepts("from").withRequiredArg().ofType(File.class).required()
                .describedAs("The csrg mapping that contains the mappings you want to merge into the to mapping.");
        parser.accepts("to").withOptionalArg().ofType(File.class)
                .describedAs("The tiny mapping that you want to merge the from mapping into.");
        parser.accepts("out").withRequiredArg().ofType(File.class).required()
                .describedAs("The file to save the merged mappings to.");
        parser.accepts("namespace").withRequiredArg().ofType(String.class).required()
                .describedAs("The namespace to use for the merged mappings.");
        parser.accepts("remap-namespace").withOptionalArg().ofType(String.class)
                .describedAs("Only available if CREATE_CSRG_FROM_TINY is selected. The namespace that will be used as the remapped one in the CSRG mappings.");

        OptionSet options = parser.parse(args);
        RunMode mode = (RunMode) options.valueOf("mode");
        File from = (File) options.valueOf("from");
        File to = (File) options.valueOf("to");
        File out = (File) options.valueOf("out");
        String namespace = (String) options.valueOf("namespace");
        String otherNamespace = (String) options.valueOf("remap-namespace");

        if(mode == RunMode.MERGE) {
            if(to == null) {
                throw new IllegalArgumentException("The to argument is required when running in MERGE mode.");
            }

            CSRGMappingFile fromFile = new CSRGMappingFile();
            fromFile.loadFromFile(from);
            TinyMappingFile toFile = new TinyMappingFile();
            toFile.loadFromFile(to);

            System.out.println("gz: " + fromFile.getRemapped(new Mapping(Mapping.Type.CLASS, "gz")));

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
        } else if(mode == RunMode.REPLACE_ORIGINAL) {
            TinyMappingFile fromFile = new TinyMappingFile();
            fromFile.loadFromFile(from);

            System.out.println(fromFile.getMapping("intermediary", new Mapping(Mapping.Type.CLASS, "l")));

            System.out.println("Replacing namespace " + namespace + " in " + from.getName() + ".");
            System.out.println("The process may take a while, please wait...");

            long start = System.currentTimeMillis();
            long startReplace = System.currentTimeMillis();
            MappingMerger.replaceOriginalNamespace(fromFile, namespace);
            long endReplace = System.currentTimeMillis();
            System.out.println("Replacing namespace took " + (endReplace - startReplace) + "ms.");
            System.out.println("Saving mappings to " + out.getName() + ".");
            System.out.println("The process may take a while, please wait...");

            long startSave = System.currentTimeMillis();
            fromFile.saveToFile(out);
            long endSave = System.currentTimeMillis();
            System.out.println("Saving mappings took " + (endSave - startSave) + "ms.");
            long end = System.currentTimeMillis();
            System.out.println("The whole process took " + (end - start) + "ms.");
        } else if(mode == RunMode.CREATE_CSRG_FROM_TINY) {
            if(otherNamespace == null) {
                throw new IllegalArgumentException("The remap-namespace argument is required when running in CREATE_CSRG_FROM_TINY mode.");
            }

            TinyMappingFile fromFile = new TinyMappingFile();
            fromFile.loadFromFile(from);

            CSRGMappingFile outMapping;

            System.out.println("Creating CSRG mappings from " + from.getName() + ".");
            System.out.println("The process may take a while, please wait...");

            long start = System.currentTimeMillis();
            long startCreate = System.currentTimeMillis();
            outMapping = MappingMerger.createCSRGfromTiny(fromFile, namespace, otherNamespace);
            long endCreate = System.currentTimeMillis();
            System.out.println("Creating CSRG mappings took " + (endCreate - startCreate) + "ms.");
            System.out.println("Saving mappings to " + out.getName() + ".");
            System.out.println("The process may take a while, please wait...");

            long startSave = System.currentTimeMillis();
            outMapping.saveToFile(out);
            long endSave = System.currentTimeMillis();
            System.out.println("Saving mappings took " + (endSave - startSave) + "ms.");
            long end = System.currentTimeMillis();
            System.out.println("The whole process took " + (end - start) + "ms.");
        }
    }
}
