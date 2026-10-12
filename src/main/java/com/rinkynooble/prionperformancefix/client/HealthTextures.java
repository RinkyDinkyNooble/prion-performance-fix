package com.rinkynooble.prionperformancefix.client;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Prion's per-mob health textures. Prion rebuilds a mob's texture pixel by pixel and uploads it on
 * every frame, but the result depends only on the mob's UUID, its health fraction and the base
 * texture. This remembers what each texture was last built from, so a frame with the same inputs
 * keeps the texture as it is.
 *
 * <p>Prion only frees a texture when its mob dies, and the death animation then builds it again.
 * Textures are freed here when the mob leaves the client's world, and all of them when the world
 * unloads.
 */
public final class HealthTextures {
    private record Built(DynamicTexture texture, ResourceLocation base, int healthBits) {
    }

    private static final Map<UUID, Built> BUILT = new HashMap<>();
    /** Prion's own texture map and its method that frees one, handed over once Prion's texture class loads. */
    private static Map<UUID, DynamicTexture> prionTextures;
    private static Consumer<UUID> prionRelease;

    private HealthTextures() {
    }

    public static void bind(Map<UUID, DynamicTexture> textures, Consumer<UUID> release) {
        prionTextures = textures;
        prionRelease = release;
    }

    /** Whether the mob's texture was last built from these same inputs. */
    public static boolean isCurrent(UUID id, float health, ResourceLocation base) {
        Built built = BUILT.get(id);
        return built != null && prionTextures != null && built.texture == prionTextures.get(id)
                && built.healthBits == Float.floatToIntBits(health) && built.base.equals(base);
    }

    public static void remember(UUID id, float health, ResourceLocation base) {
        DynamicTexture texture = prionTextures != null ? prionTextures.get(id) : null;
        if (texture != null) {
            BUILT.put(id, new Built(texture, base, Float.floatToIntBits(health)));
        } else {
            BUILT.remove(id);
        }
    }

    public static void release(UUID id) {
        BUILT.remove(id);
        if (prionTextures != null && prionTextures.containsKey(id)) {
            prionRelease.accept(id);
        }
    }

    public static void releaseAll() {
        BUILT.clear();
        if (prionTextures != null) {
            for (UUID id : List.copyOf(prionTextures.keySet())) {
                prionRelease.accept(id);
            }
        }
    }
}
