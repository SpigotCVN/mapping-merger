package io.github.spigotcvn.merger.mappings.files;

import io.github.spigotcvn.merger.mappings.InvalidMappingFormatException;
import io.github.spigotcvn.merger.mappings.types.Mapping;
import javafx.util.Pair;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class TinyMappingFile implements Loadable, Saveable {
    private String originalNamespace;
    // structure: Map (String namespace, List (Pair (Mapping from, Mapping to)))
    private final Map<String, List<Pair<Mapping, Mapping>>> namespaces = new LinkedHashMap<>();

    public void addNamespace(String namespace) {
        if(namespace == null || namespaces.containsKey(namespace)) {
            throw new IllegalArgumentException("Invalid namespace: " + namespace);
        }
        namespaces.put(namespace, new ArrayList<>());
    }

    public Mapping getMapping(String namespace, Mapping from) {
        if(namespace == null || from == null) {
            throw new IllegalArgumentException("Invalid arguments: " + namespace + ", " + from);
        }

        if(namespace.equals(originalNamespace)) {
            return from;
        }
        List<Pair<Mapping, Mapping>> mappings = namespaces.get(namespace);
        if(mappings == null) {
            throw new IllegalArgumentException("Unknown namespace: " + namespace);
        }
        for (Pair<Mapping, Mapping> pair : mappings) {
            if(pair == null) {
                throw new IllegalStateException("Pair is null");
            }
            if(pair.getKey().equals(from)) {
                return pair.getValue();
            }
        }
        return null;
    }

    public Mapping getOriginal(String namespace, Mapping to) {
        if(namespace == null || to == null) {
            throw new IllegalArgumentException("Invalid arguments: " + namespace + ", " + to);
        }

        if(namespace.equals(originalNamespace)) {
            return to;
        }
        List<Pair<Mapping, Mapping>> mappings = namespaces.get(namespace);
        if(mappings == null) {
            throw new IllegalArgumentException("Unknown namespace: " + namespace);
        }
        for (Pair<Mapping, Mapping> pair : mappings) {
            if(pair.getValue().equals(to)) {
                return pair.getKey();
            }
        }
        return null;
    }

    public void addMapping(String namespace, Mapping from, Mapping to) {
        if(namespace == null || from == null || to == null) {
            throw new IllegalArgumentException("Invalid arguments: " + namespace + ", " + from + ", " + to);
        }

        if(namespace.equals(originalNamespace)) {
            throw new IllegalArgumentException("Cannot add mapping to the original namespace: " + namespace);
        }
        List<Pair<Mapping, Mapping>> mappings = namespaces.get(namespace);
        if(mappings == null) {
            throw new IllegalArgumentException("Unknown namespace: " + namespace);
        }
        mappings.add(new Pair<>(from, to));
    }

    public List<Mapping> getOriginalMappings() {
        List<Mapping> mappings = new ArrayList<>();
        List<Pair<Mapping, Mapping>> firstNamespaceMappings = namespaces.get(namespaces.keySet().iterator().next());
        for (Pair<Mapping, Mapping> pair : firstNamespaceMappings) {
            mappings.add(pair.getKey());
        }
        return mappings;
    }

    public List<Mapping> getMappings(String namespace) {
        if(namespace == null) {
            throw new IllegalArgumentException("Invalid namespace: " + namespace);
        }

        if(namespace.equals(originalNamespace)) {
            return getOriginalMappings();
        }

        if(!namespaces.containsKey(namespace)) {
            throw new IllegalArgumentException("Unknown namespace: " + namespace);
        }

        List<Mapping> mappings = new ArrayList<>();
        List<Pair<Mapping, Mapping>> namespaceMappings = namespaces.get(namespace);
        for (Pair<Mapping, Mapping> pair : namespaceMappings) {
            mappings.add(pair.getValue());
        }
        return mappings;
    }

    @Override
    public void loadFromStream(InputStream is) throws InvalidMappingFormatException {
        // check if namespaces has any nulls
        namespaces.values().forEach(mappings -> {
            mappings.forEach(pair -> {
                if(pair == null || pair.getKey() == null || pair.getValue() == null) {
                    System.err.println("Has nulls: " + pair);
                }
            });
        });

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            int lineCount = 0;
            String line;
            List<String> namespaces = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                if(lineCount == 0 && !line.startsWith("v1")) {
                    throw new InvalidMappingFormatException("Invalid version: " + line);
                } else if(lineCount == 0) {
                    String formatted = line.replaceFirst("v1\t", "").trim();
                    String[] parts = formatted.split("\t");
                    namespaces.addAll(Arrays.asList(parts));
                    originalNamespace = namespaces.get(0);
                    namespaces.remove(0);
                    for(String namespace : namespaces) {
                        addNamespace(namespace);
                    }
                    lineCount++;
                    continue;
                }

                String[] parts = line.split("\t");
                if(parts.length < namespaces.size() + 1) {
                    throw new InvalidMappingFormatException("Invalid line: " + line);
                }
                if(parts[0].equals("CLASS")) {
                    for (int i = 1; i < parts.length - 1; i++) {
                        addMapping(
                                namespaces.get(i - 1),
                                new Mapping(
                                        Mapping.Type.CLASS,
                                        parts[1],
                                        null,
                                        null
                                ),
                                new Mapping(
                                        Mapping.Type.CLASS,
                                        parts[i + 1],
                                        null,
                                        null
                                )
                        );
                    }
                }
                if(parts[0].equals("FIELD")) {
                    for (int i = 3; i < parts.length - 1; i++) {
                        addMapping(
                                namespaces.get(i - 3),
                                new Mapping(
                                        Mapping.Type.FIELD,
                                        parts[1],
                                        parts[3],
                                        parts[2]
                                ),
                                new Mapping(
                                        Mapping.Type.FIELD,
                                        parts[i + 1],
                                        parts[3],
                                        parts[2]
                                )
                        );
                    }
                }
                if(parts[0].equals("METHOD")) {
                    for (int i = 3; i < parts.length - 1; i++) {
                        addMapping(
                                namespaces.get(i - 3),
                                new Mapping(
                                        Mapping.Type.METHOD,
                                        parts[1],
                                        parts[3],
                                        parts[2]
                                ),
                                new Mapping(
                                        Mapping.Type.METHOD,
                                        parts[i + 1],
                                        parts[3],
                                        parts[2]
                                )
                        );
                    }
                }

                lineCount++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveToStream(OutputStream os) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os), 1024 * 1024)) {
            StringBuilder sb = new StringBuilder();

            // Write header
            sb.append("# Generated by MappingMerger").append("\n");
            sb.append("v1").append("\t").append(originalNamespace);
            for (String namespace : namespaces.keySet()) {
                sb.append("\t").append(namespace);
            }
            sb.append("\n");
            writer.write(sb.toString());

            // ReList StringBuilder for reuse
            sb.setLength(0);

            // Cache namespace entry Lists
            List<String> namespaceKeys = new ArrayList<>(namespaces.keySet());
            List<Pair<Mapping, Mapping>> firstNamespaceMappings = namespaces.get(namespaceKeys.get(0));

            List<String> lines = firstNamespaceMappings.parallelStream()
                    .map(pair -> {
                        StringBuilder lineBuilder = new StringBuilder();
                        Mapping from = pair.getKey();

                        lineBuilder.append(from.getType().name()).append("\t")
                                .append(from.getName());
                        if (from.getDescriptor() != null) {
                            lineBuilder.append("\t").append(from.getDescriptor());
                        }
                        if (from.getClassName() != null) {
                            lineBuilder.append("\t").append(from.getClassName());
                        }

                        for (String namespace : namespaceKeys) {
                            Mapping to = getMapping(namespace, from);
                            if (to == null) {
                                throw new IllegalStateException("Missing mapping for " + from + " in " + namespace);
                            }
                            lineBuilder.append("\t").append(to.getName());
                        }
                        lineBuilder.append("\n");
                        return lineBuilder.toString();
                    })
                    .collect(Collectors.toList());

            // Write all lines to the writer
            writer.write(String.join("", lines));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
