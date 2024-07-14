package io.github.spigotcvn.merger.mappings.files;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.github.spigotcvn.merger.mappings.InvalidMappingFormatException;
import io.github.spigotcvn.merger.mappings.types.Mapping;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class TinyMappingFile implements Loadable, Saveable {
    private String originalNamespace;
    // structure: Map (String namespace, List (Pair (Mapping from, Mapping to)))
    private Map<String, BiMap<Mapping, Mapping>> namespaces = new LinkedHashMap<>();

    public void addNamespace(String namespace) {
        if(namespace == null || namespaces.containsKey(namespace)) {
            throw new IllegalArgumentException("Invalid namespace: " + namespace);
        }
        namespaces.put(namespace, HashBiMap.create());
    }

    public void removeNamespace(String namespace) {
        if(namespace == null || !namespaces.containsKey(namespace)) {
            throw new IllegalArgumentException("Invalid namespace: " + namespace);
        }
        namespaces.remove(namespace);
    }

    public Mapping getMapping(String namespace, Mapping from) {
        if(namespace == null || from == null) {
            throw new IllegalArgumentException("Invalid arguments: " + namespace + ", " + from);
        }

        if(namespace.equals(originalNamespace)) {
            return from;
        }
        Map<Mapping, Mapping> mappings = namespaces.get(namespace);
        if(mappings == null) {
            throw new IllegalArgumentException("Unknown namespace: " + namespace);
        }
        return mappings.get(from);
    }

    public Mapping getMappingFromNamespace(String fromNamespace, String namespace, Mapping fromMapping) {
        // this needs to get the mappings original form and then get the mapping from the namespace
        if(fromNamespace == null || namespace == null || fromMapping == null) {
            throw new IllegalArgumentException("Invalid arguments: " + fromNamespace + ", " + namespace + ", " + fromMapping);
        }

        if(fromNamespace.equals(namespace)) {
            return fromMapping;
        }

        if(fromNamespace.equals(originalNamespace)) {
            return getMapping(namespace, fromMapping);
        }

        Mapping original = getOriginal(fromNamespace, fromMapping);
        if(original == null) {
            return null;
        }

        return getMapping(namespace, original);
    }

    public Mapping getOriginal(String namespace, Mapping to) {
        if(namespace == null || to == null) {
            throw new IllegalArgumentException("Invalid arguments: " + namespace + ", " + to);
        }

        if(namespace.equals(originalNamespace)) {
            return to;
        }

        BiMap<Mapping, Mapping> mappings = namespaces.get(namespace);
        if(mappings == null) {
            throw new IllegalArgumentException("Unknown namespace: " + namespace);
        }
        return mappings.inverse().get(to);
    }

    public void addMapping(String namespace, Mapping from, Mapping to) {
        if(namespace == null || from == null || to == null) {
            throw new IllegalArgumentException("Invalid arguments: " + namespace + ", " + from + ", " + to);
        }

        if(namespace.equals(originalNamespace)) {
            throw new IllegalArgumentException("Cannot add mapping to the original namespace: " + namespace);
        }

        Map<Mapping, Mapping> mappings = namespaces.get(namespace);
        if(mappings == null) {
            throw new IllegalArgumentException("Unknown namespace: " + namespace);
        }
        mappings.put(from, to);
    }

    public List<Mapping> getOriginalMappings() {
        List<Mapping> mappings = new ArrayList<>();
        Map<Mapping, Mapping> firstNamespace = namespaces.get(namespaces.keySet().iterator().next());
        firstNamespace.forEach((from, to) -> mappings.add(from));
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
        Map<Mapping, Mapping> namespaceMappings = namespaces.get(namespace);
        namespaceMappings.forEach((from, to) -> mappings.add(to));
        return mappings;
    }

    public Map<String, Map<Mapping, Mapping>> getNamespaces() {
        // create a deep copy of the namespaces map
        Map<String, Map<Mapping, Mapping>> copy = new LinkedHashMap<>();
        for(Map.Entry<String, BiMap<Mapping, Mapping>> entry : namespaces.entrySet()) {
            BiMap<Mapping, Mapping> mappings = HashBiMap.create();
            mappings.putAll(entry.getValue());
            copy.put(entry.getKey(), mappings);
        }
        return copy;
    }

    public String getOriginalNamespaceName() {
        return originalNamespace;
    }

    public void setOriginalNamespaceName(String originalNamespace) {
        this.originalNamespace = originalNamespace;
    }

    @Override
    public void loadFromStream(InputStream is) throws InvalidMappingFormatException {
        namespaces.clear();
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
                    // following format: FIELD className descriptor originalName otherName
                    for (int i = 3; i < parts.length - 1; i++) {
                        addMapping(
                                namespaces.get(i - 3),
                                new Mapping(
                                        Mapping.Type.FIELD,
                                        parts[3],
                                        parts[1],
                                        parts[2]
                                ),
                                new Mapping(
                                        Mapping.Type.FIELD,
                                        parts[i + 1],
                                        parts[1],
                                        parts[2]
                                )
                        );
                    }
                }
                if(parts[0].equals("METHOD")) {
                    // following format: METHOD className descriptor originalName otherName
                    for (int i = 3; i < parts.length - 1; i++) {
                        addMapping(
                                namespaces.get(i - 3),
                                new Mapping(
                                        Mapping.Type.METHOD,
                                        parts[3],
                                        parts[1],
                                        parts[2]
                                ),
                                new Mapping(
                                        Mapping.Type.METHOD,
                                        parts[i + 1],
                                        parts[1],
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
            Map<Mapping, Mapping> firstNamespaceMappings = namespaces.get(namespaceKeys.get(0));

            List<String> lines = firstNamespaceMappings.entrySet().parallelStream()
                    .map(pair -> {
                        StringBuilder lineBuilder = new StringBuilder();
                        Mapping from = pair.getKey();

                        lineBuilder.append(from.getType().name());
                        if (from.getClassName() != null) {
                            lineBuilder.append("\t").append(from.getClassName());
                        }
                        if (from.getDescriptor() != null) {
                            lineBuilder.append("\t").append(from.getDescriptor());
                        }
                        lineBuilder.append("\t").append(from.getName());

                        for (String namespace : namespaceKeys) {
                            Mapping to = getMapping(namespace, from);
                            if (to == null) {
                                System.out.println("Missing mapping for " + from + " in " + namespace);
                                continue;
//                                throw new IllegalStateException("Missing mapping for " + from + " in " + namespace);
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
