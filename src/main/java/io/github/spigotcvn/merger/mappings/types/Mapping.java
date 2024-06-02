package io.github.spigotcvn.merger.mappings.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Mapping {
    @NotNull
    private final Type type;
    @NotNull
    private final String name;
    @Nullable
    private final String className;
    @Nullable
    private final String descriptor;

    public Mapping(@NotNull Type type, @NotNull String name, @Nullable String className, @Nullable String descriptor) {
        this.type = type;
        this.name = name;
        this.className = className;
        this.descriptor = descriptor;
    }

    public Mapping(@NotNull Type type, @NotNull String name, @Nullable String className) {
        this(type, name, className, null);
    }

    public Mapping(@NotNull Type type, @NotNull String name) {
        this(type, name, null, null);
    }

    @NotNull
    public Type getType() {
        return type;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Nullable
    public String getClassName() {
        return className;
    }

    @Nullable
    public String getDescriptor() {
        return descriptor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, className, descriptor);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Mapping mapping = (Mapping) obj;
        return type == mapping.type && name.equals(mapping.name) && Objects.equals(className, mapping.className) && Objects.equals(descriptor, mapping.descriptor);
    }

    @Override
    public String toString() {
        return "Mapping{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", className='" + className + '\'' +
                ", descriptor='" + descriptor + '\'' +
                '}';
    }

    public enum Type {
        CLASS, FIELD, METHOD
    }
}