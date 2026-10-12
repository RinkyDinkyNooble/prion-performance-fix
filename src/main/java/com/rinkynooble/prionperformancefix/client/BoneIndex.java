package com.rinkynooble.prionperformancefix.client;

import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.function.Function;

/**
 * Answers {@code HierarchicalModel.getAnyDescendantWithName} from a name-to-part map built once
 * per model root, instead of streaming the whole part tree for every animated bone on every frame.
 *
 * <p>Minecraft walks the tree depth first (a part, then each child in its map's order) and returns
 * the named child of the first part that has one. The map is filled in that same order and keeps
 * the first part found for each name, so it gives the same answer. Trees it can't index exactly
 * that way (part subclasses, which may override the search, empty or repeated entries, or
 * children maps with their own key comparison) are left to Minecraft's search.
 */
public final class BoneIndex {
    /** Marks a root whose lookups Minecraft's own search answers. */
    private static final Map<String, Optional<ModelPart>> UNSUPPORTED = Collections.unmodifiableMap(new HashMap<>());

    /** Added to {@link ModelPart} by {@code ModelPartMixin}. */
    public interface Part {
        Map<String, ModelPart> prionperformancefix$children();

        @Nullable
        Map<String, Optional<ModelPart>> prionperformancefix$boneIndex();

        void prionperformancefix$setBoneIndex(Map<String, Optional<ModelPart>> index);

        @Nullable
        List<ModelPart> prionperformancefix$allParts();

        void prionperformancefix$setAllParts(List<ModelPart> parts);
    }

    private BoneIndex() {
    }

    /** The part {@code name} finds under {@code root}, or null when Minecraft's own search has to answer. */
    @Nullable
    public static Optional<ModelPart> find(ModelPart root, String name) {
        if (!((Object) root instanceof Part holder)) {
            return null;
        }
        Map<String, Optional<ModelPart>> index = holder.prionperformancefix$boneIndex();
        if (index == null) {
            index = build(root, part -> ((Part) (Object) part).prionperformancefix$children());
            if (index == null) {
                index = UNSUPPORTED;
            }
            holder.prionperformancefix$setBoneIndex(index);
        }
        if (index == UNSUPPORTED) {
            return null;
        }
        Optional<ModelPart> found = index.get(name);
        return found != null ? found : Optional.empty();
    }

    /** Every child name in the tree mapped to the part Minecraft's search returns for it, or null if the tree isn't supported. */
    @Nullable
    public static Map<String, Optional<ModelPart>> build(ModelPart root, Function<ModelPart, Map<String, ModelPart>> childrenOf) {
        Map<String, Optional<ModelPart>> found = new HashMap<>();
        if (!collect(root, childrenOf, found, new IdentityHashMap<>())) {
            return null;
        }
        return Map.copyOf(found);
    }

    private static boolean collect(ModelPart part, Function<ModelPart, Map<String, ModelPart>> childrenOf,
                                   Map<String, Optional<ModelPart>> found, Map<ModelPart, Boolean> visited) {
        if (part.getClass() != ModelPart.class || visited.put(part, Boolean.TRUE) != null) {
            return false;
        }
        Map<String, ModelPart> children = childrenOf.apply(part);
        if (children == null || children instanceof SortedMap<String, ModelPart> sorted && sorted.comparator() != null) {
            return false;
        }
        for (Map.Entry<String, ModelPart> entry : children.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                return false;
            }
            found.putIfAbsent(entry.getKey(), Optional.of(entry.getValue()));
        }
        // Stream order, as Minecraft's search uses: some maps (fastutil's open hash maps) iterate in a different order.
        for (ModelPart child : children.values().stream().toList()) {
            if (!collect(child, childrenOf, found, visited)) {
                return false;
            }
        }
        return true;
    }
}
