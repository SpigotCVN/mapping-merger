package io.github.spigotcvn.merger;

import io.github.spigotcvn.merger.mappings.files.CSRGMappingFile;
import io.github.spigotcvn.merger.mappings.files.TinyMappingFile;
import io.github.spigotcvn.merger.mappings.types.Mapping;
import io.github.spigotcvn.merger.util.Pair;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MappingMerger {
    /**
     * Merges the tiny mappings with the CSRG mappings.
     * <p>
     * Explanation: All mappings have an original mapping, or something I call root mapping
     * It's the namespace that the mapping is initially in, usually the one we remap from
     * A mapping can have multiple namespaces, but they all have the same root mapping
     * and are just renamed forms of the root mapping.
     * Csrg does not have multiple namespaces, it only has a root mapping and a renamed form of it.
     * Tiny mappings have multiple namespaces, but they all have the same root mapping.
     * If the csrg mapping has the same root mapping as the tiny mapping, we can merge them by
     * adding a namespace to the tiny mapping and then adding the names from the csrg mapping
     * into that namespace.
     * <p>
     * If a field, method or class doesn't exist in the csrg mapping, it will be set to the name
     * it has in the original namespace in the tiny mapping.
     * @param tiny The tiny mappings
     * @param csrg The csrg mappings
     * @param newNamespaceName The name of the new namespace that will be added to the tiny mappings
     */
    public static void mergeTinyWithCSRG(TinyMappingFile tiny, CSRGMappingFile csrg, String newNamespaceName) {
        List<Pair<Mapping, Mapping>> remapped = tiny.getOriginalMappings().parallelStream().map(originalMapping -> {
            Mapping remappedMapping = csrg.getRemapped(originalMapping);

            if (remappedMapping == null && originalMapping.getType() == Mapping.Type.FIELD) {
                // Fields in csrg do not have a descriptor, only name and class name
                remappedMapping = csrg.getRemapped(new Mapping(Mapping.Type.FIELD, originalMapping.getName(), originalMapping.getClassName()));
            }

            if (remappedMapping == null) {
                remappedMapping = originalMapping;
            }

            return new Pair<>(originalMapping, remappedMapping);
        }).collect(Collectors.toList());

        tiny.addNamespace(newNamespaceName);
        remapped.forEach(pair -> tiny.addMapping(newNamespaceName, pair.getKey(), pair.getValue()));
    }

    /**
     * Replaces a namespace in the tiny mappings.
     * This means that you can for example make the namespace called official take all fields from the namespace called named
     * spigot into itself, deleting the namespace called spigot.
     * @param tiny The tiny mappings
     * @param namespaceToReplace The namespace that should be replaced
     * @param namespaceReplacedBy The namespace that should replace the other namespace
     */
    public static void replaceNamespace(TinyMappingFile tiny, String namespaceToReplace, String namespaceReplacedBy) {
        if(namespaceToReplace.equals(namespaceReplacedBy)) {
            return;
        }

        Map<Mapping, Mapping> mappings;

        List<Mapping> original = tiny.getOriginalMappings();

        mappings = original.parallelStream().map(mapping -> {
            Mapping remapped = tiny.getMapping(namespaceReplacedBy, mapping);
            if(remapped == null) {
                remapped = mapping;
            }

            return new Pair<>(mapping, remapped);
        }).collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        tiny.removeNamespace(namespaceToReplace);
        tiny.addNamespace(namespaceToReplace);

        mappings.forEach((originalMapping, remappedMapping) -> tiny.addMapping(namespaceToReplace, originalMapping, remappedMapping));
    }

    /**
     * Replaces the original namespace in the tiny mappings with another namespace.
     * Due to how the {@link TinyMappingFile} class is implemented, this is a bit more complex than just replacing the namespace.
     * The original namespace doesn't exist as a separate namespace, but only exists in every single stored
     * Map with mappings as the key.
     * This means that we have to go through every single mapping and replace the original namespace with the new namespace.
     * Additionally, any references of classes in the class names or descriptors of the mappings will be replaced.
     * @param tiny The tiny mappings
     * @param namespaceReplacedBy The namespace that should replace the original namespace
     * @param newOriginalMappingName The name of the new original namespace
     */
    public static void replaceOriginalNamespace(TinyMappingFile tiny, String namespaceReplacedBy, String newOriginalMappingName) {
        Map<String, Map<Mapping, Mapping>> namespaces = tiny.getNamespaces();
        Map<Mapping, Mapping> replaceBy = new LinkedHashMap<>();
        tiny.getOriginalMappings().forEach(mapping -> {
            Mapping remapped = tiny.getMapping(namespaceReplacedBy, mapping);
            if (remapped == null) {
                remapped = mapping;
            }

            replaceBy.put(mapping, remapped);
        });

        namespaces.forEach((key, value) -> tiny.removeNamespace(key));

        Map<String, Map<Mapping, Mapping>> newNamespaces = new LinkedHashMap<>();
        namespaces.forEach((key, value) -> {
            if (key.equals(namespaceReplacedBy)) {
                return;
            }
            Map<Mapping, Mapping> newMappings = new LinkedHashMap<>();
            value.forEach((originalMapping, remappedMapping) -> {
                Mapping newRemapped = replaceBy.get(originalMapping);
                if (newRemapped == null) {
                    newRemapped = remappedMapping;
                }

                // Replace class names and descriptors in the newRemapped mapping
                newRemapped = replaceClassReferences(newRemapped, replaceBy);
//                System.out.println(newRemapped.getClassName());

                newMappings.put(newRemapped, remappedMapping);
            });

            newNamespaces.put(key, newMappings);
        });

        tiny.setOriginalNamespaceName(newOriginalMappingName);
        newNamespaces.forEach((key, value) -> {
            tiny.addNamespace(key);
            value.forEach((originalMapping, remappedMapping) -> tiny.addMapping(key, originalMapping, remappedMapping));
        });
    }

    /**
     * Replaces class references in the class names or descriptors of the mapping.
     * @param mapping The mapping to replace class references in
     * @param replaceBy The map of original mappings to their replacements
     * @return The mapping with replaced class references
     */
    private static Mapping replaceClassReferences(Mapping mapping, Map<Mapping, Mapping> replaceBy) {
        String className = mapping.getClassName();
        String descriptor = mapping.getDescriptor();

        if (className != null) {
            Mapping remappedClassName = replaceBy.get(new Mapping(Mapping.Type.CLASS, className));
            if (remappedClassName != null) {
                className = remappedClassName.getName();
            }
        }

        // Replace descriptor class names
        if (descriptor != null) {
//            System.out.println("Descriptor: " + descriptor);
            descriptor = replaceClassNamesInDescriptor(descriptor, replaceBy);
        }

//        System.out.println("Original mapping: " + mapping);
//        System.out.println("getClassName(): " + mapping.getClassName());
//        System.out.println("Class name: " + className);
//        System.out.println("Descriptor: " + descriptor);

        return new Mapping(mapping.getType(), mapping.getName(), className, descriptor);
    }

    /**
     * Replaces class names in a descriptor string using the provided map of replacements.
     * @param descriptor The descriptor string to replace class names in
     * @param replaceBy The map of original mappings to their replacements
     * @return The descriptor string with replaced class names
     */
    private static String replaceClassNamesInDescriptor(String descriptor, Map<Mapping, Mapping> replaceBy) {
        if (descriptor == null || descriptor.isEmpty()) {
            return descriptor;
        }

        StringBuilder newDescriptor = new StringBuilder();
        int length = descriptor.length();
        for (int i = 0; i < length; i++) {
            char c = descriptor.charAt(i);
            if (c == 'L') {
                int start = i;
                int end = descriptor.indexOf(';', start);
                if (end != -1) {
                    String className = descriptor.substring(start + 1, end);
                    Mapping remappedClassName = replaceBy.get(new Mapping(Mapping.Type.CLASS, className));
                    if (remappedClassName != null) {
                        className = remappedClassName.getName();
                    }
                    newDescriptor.append('L').append(className).append(';');
                    i = end;
                    continue;
                }
            }
            newDescriptor.append(c);
        }

        return newDescriptor.toString();
    }

    /**
     * Calls {@link MappingMerger#replaceOriginalNamespace(TinyMappingFile, String, String)},
     * please see that method for more information.
     * @see MappingMerger#replaceOriginalNamespace(TinyMappingFile, String, String)
     */
    public static void replaceOriginalNamespace(TinyMappingFile tiny, String namespaceReplacedBy) {
        replaceOriginalNamespace(tiny, namespaceReplacedBy, namespaceReplacedBy);
    }

    public static CSRGMappingFile createCSRGfromTiny(TinyMappingFile tiny, String origNamespace, String remapNamespace) {
        CSRGMappingFile csrg = new CSRGMappingFile();

        List<Mapping> mappings = tiny.getMappings(origNamespace);
        Map<String, Map<Mapping, Mapping>> namespaces = tiny.getNamespaces();

        Map<Mapping, Mapping> mappingMap = mappings.parallelStream().map(mapping -> {
            Mapping remapped = tiny.getMappingFromNamespace(origNamespace, remapNamespace, mapping);
            if (remapped == null) {
                remapped = mapping;
            }

            // replace the classname and descriptor to match the new original name (mapping)
            Mapping newMapping = replaceClassReferences(mapping, namespaces.get(origNamespace));
            remapped = replaceClassReferences(remapped, namespaces.get(origNamespace));

            return new Pair<>(newMapping, remapped);
        }).collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        mappingMap.forEach(csrg::addMapping);

        return csrg;
    }

    /**
     * Applies a package mapping to the tiny file.
     * Package mappings work in the following way:<p>
     *     There is a csrg or srg file which contains class mappings which will be used as
     *     package mappings.
     *     E.g. if it contains "me/andreasmelone/ io/github/spigotcvn" it will change all classes
     *     that use me/andreasmelone/ to use io/github/spigotcvn.
     *     ./ will turn package-less classes to have packages
     * @param toApplyTo The mapping file to apply the package mapping to
     * @param packageMapping The package mapping itself
     */
    public static void applyPackageMapping(TinyMappingFile toApplyTo, CSRGMappingFile packageMapping) {
        packageMapping.forEach((from, to) -> {
            if(from.getType() != Mapping.Type.CLASS || to.getType() != Mapping.Type.CLASS) {
                return;
            }

            String fromName = from.getName();
            String toName = to.getName();

            // csrg mapping is iterable, tiny mapping isn't
            Map<String, Map<Mapping, Mapping>> namespaces = new LinkedHashMap<>();
            toApplyTo.getNamespaces().forEach((namespace, mappings) -> {
                Map<Mapping, Mapping> newMapping = new LinkedHashMap<>();
                mappings.forEach((original, remapped) -> {
                    if(remapped.getClassName() != null) {
                        String newClassName = remapped.getClassName();
                        if(fromName.equals("./")) {
                            newClassName = toName + "/" + newClassName;
                        } else {
                            newClassName = newClassName.replace(fromName, toName);
                        }
                        newMapping.put(original, new Mapping(remapped.getType(), remapped.getName(), newClassName, remapped.getDescriptor()));
                    }
                });
                namespaces.put(namespace, newMapping);
            });

            namespaces.forEach((namespace, mappings) -> {
                toApplyTo.removeNamespace(namespace);
                toApplyTo.addNamespace(namespace);
                mappings.forEach((original, remapped) -> {
                    toApplyTo.addMapping(namespace, original, remapped);
                });
            });
        });
    }
}
