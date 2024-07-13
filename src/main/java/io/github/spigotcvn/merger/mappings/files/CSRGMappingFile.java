package io.github.spigotcvn.merger.mappings.files;

import io.github.spigotcvn.merger.mappings.InvalidMappingFormatException;
import io.github.spigotcvn.merger.mappings.types.Mapping;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CSRGMappingFile implements Loadable, Saveable, Iterable<Map.Entry<Mapping, Mapping>> {
    private final Map<Mapping, Mapping> orig2remap = new LinkedHashMap<>();

    public void addMapping(Mapping from, Mapping to) {
        orig2remap.put(from, to);
    }

    public Mapping getRemapped(Mapping from) {
        if(from == null) {
            throw new IllegalArgumentException("Mapping cannot be null");
        }

        return orig2remap.get(from);
    }

    public Mapping getOriginal(Mapping to) {
        if(to == null) {
            throw new IllegalArgumentException("Mapping cannot be null");
        }

        for(Map.Entry<Mapping, Mapping> entry : orig2remap.entrySet()) {
            if(entry.getValue().equals(to)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public List<Mapping> getOriginalMappings() {
        List<Mapping> original = new ArrayList<>();
        orig2remap.forEach((from, to) -> original.add(from));
        return original;
    }

    public List<Mapping> getRemappedMappings() {
        List<Mapping> remapped = new ArrayList<>();
        orig2remap.forEach((from, to) -> remapped.add(to));
        return remapped;
    }

    public void loadFromStream(InputStream is) throws InvalidMappingFormatException {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split(" ");
                if (parts.length < 2) {
                    throw new InvalidMappingFormatException("Invalid line: " + line);
                }
                if(parts.length == 2) {
                    // this is a class mapping
                    Mapping from = new Mapping(Mapping.Type.CLASS, parts[0], null, null);
                    Mapping to = new Mapping(Mapping.Type.CLASS, parts[1], null, null);
                    this.addMapping(from, to);
                } else if(parts.length == 3) {
                    // this is a field mapping
                    Mapping from = new Mapping(Mapping.Type.FIELD, parts[1], parts[0], null);
                    Mapping to = new Mapping(Mapping.Type.FIELD, parts[2], parts[0], null);
                    this.addMapping(from, to);
                } else if(parts.length == 4) {
                    // this is a method mapping
                    Mapping from = new Mapping(Mapping.Type.METHOD, parts[1], parts[0], parts[2]);
                    Mapping to = new Mapping(Mapping.Type.METHOD, parts[3], parts[0], parts[2]);
                    this.addMapping(from, to);
                } else {
                    throw new InvalidMappingFormatException("Invalid line: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveToStream(OutputStream os) {
        try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os))) {

            orig2remap.forEach((from, to) -> {
                if(from == null || to == null) {
                    throw new IllegalStateException("Missing mapping");
                }

                try {
                    if (from.getType() == Mapping.Type.CLASS) {
                        writer.append(from.getName()).append(" ")
                                .append(to.getName()).append(System.lineSeparator());
                    } else if (from.getType() == Mapping.Type.FIELD) {
                        writer.append(from.getName()).append(" ")
                                .append(from.getClassName()).append(" ")
                                .append(to.getName()).append(System.lineSeparator());
                    } else if (from.getType() == Mapping.Type.METHOD) {
                        writer.append(from.getName()).append(" ")
                                .append(from.getClassName()).append(" ")
                                .append(from.getDescriptor()).append(" ")
                                .append(to.getName()).append(System.lineSeparator());
                    } else {
                        throw new IllegalStateException("Invalid mapping types");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NotNull
    @Override
    public Iterator<Map.Entry<Mapping, Mapping>> iterator() {
        return orig2remap.entrySet().iterator();
    }

    public void forEach(BiConsumer<Mapping, Mapping> action) {
        orig2remap.forEach(action);
    }
}
