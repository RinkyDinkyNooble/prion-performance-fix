package com.rinkynooble.prionperformancefix.client;

import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Answers {@code ModelPart.getAllParts} from a list built once per part, instead of nested streams
 * over the whole tree. Models call it on every frame to reset their pose before animating.
 *
 * <p>The list holds the same parts in the same order as Minecraft's stream: the part, then each
 * child's parts in the order of the children map's stream. Trees with part subclasses (which may
 * override it), empty or repeated entries are left to Minecraft's code.
 */
public final class AllParts {
    /** Marks a part whose list Minecraft's own code gives. */
    private static final List<ModelPart> UNSUPPORTED = Collections.unmodifiableList(new ArrayList<>());

    private AllParts() {
    }

    /** The parts under {@code part}, itself first, or null when Minecraft's own code has to answer. */
    @Nullable
    public static List<ModelPart> of(ModelPart part) {
        if (!((Object) part instanceof BoneIndex.Part holder)) {
            return null;
        }
        List<ModelPart> parts = holder.prionperformancefix$allParts();
        if (parts == null) {
            parts = build(part, child -> ((BoneIndex.Part) (Object) child).prionperformancefix$children());
            if (parts == null) {
                parts = UNSUPPORTED;
            }
            holder.prionperformancefix$setAllParts(parts);
        }
        return parts == UNSUPPORTED ? null : parts;
    }

    /** The parts Minecraft's stream gives, in its order, or null if the tree isn't supported. */
    @Nullable
    public static List<ModelPart> build(ModelPart part, Function<ModelPart, Map<String, ModelPart>> childrenOf) {
        List<ModelPart> parts = new ArrayList<>();
        if (!collect(part, childrenOf, parts, new IdentityHashMap<>())) {
            return null;
        }
        return List.copyOf(parts);
    }

    private static boolean collect(ModelPart part, Function<ModelPart, Map<String, ModelPart>> childrenOf,
                                   List<ModelPart> parts, Map<ModelPart, Boolean> visited) {
        if (part.getClass() != ModelPart.class || visited.put(part, Boolean.TRUE) != null) {
            return false;
        }
        Map<String, ModelPart> children = childrenOf.apply(part);
        if (children == null) {
            return false;
        }
        parts.add(part);
        for (ModelPart child : children.values().stream().toList()) {
            if (child == null || !collect(child, childrenOf, parts, visited)) {
                return false;
            }
        }
        return true;
    }
}
