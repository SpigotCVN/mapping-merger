package io.github.spigotcvn.merger;

import io.github.spigotcvn.merger.mappings.files.CSRGMappingFile;
import io.github.spigotcvn.merger.mappings.files.TinyMappingFile;
import io.github.spigotcvn.merger.mappings.types.Mapping;
import io.github.spigotcvn.merger.util.Pair;

import java.util.List;
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
}
