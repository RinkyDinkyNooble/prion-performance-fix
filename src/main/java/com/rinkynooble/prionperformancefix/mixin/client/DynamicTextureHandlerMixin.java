package com.rinkynooble.prionperformancefix.mixin.client;

import com.rinkynooble.prionperformancefix.client.HealthTextures;
import net.demod.prionmod.entity.client.texturehandling.DynamicTextureHandler;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

/**
 * Prion's health textures: each mob's texture is only rebuilt and uploaded when its health
 * fraction or base texture changed since the last build (see {@link HealthTextures}).
 */
@Mixin(value = DynamicTextureHandler.class, remap = false)
public abstract class DynamicTextureHandlerMixin {
    @Shadow
    @Final
    private static Map<UUID, DynamicTexture> textures;

    @Shadow
    public static void removeTextureForEntity(UUID entityUUID) {
        throw new AssertionError();
    }

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void prionperformancefix$bind(CallbackInfo ci) {
        HealthTextures.bind(textures, id -> removeTextureForEntity(id));
    }

    @Inject(method = "updateTextureForEntity", at = @At("HEAD"), cancellable = true)
    private static void prionperformancefix$skipUnchanged(UUID id, LivingEntity entity, float health, ResourceLocation base, CallbackInfo ci) {
        if (HealthTextures.isCurrent(id, health, base)) {
            ci.cancel();
        }
    }

    @Inject(method = "updateTextureForEntity", at = @At("RETURN"))
    private static void prionperformancefix$remember(UUID id, LivingEntity entity, float health, ResourceLocation base, CallbackInfo ci) {
        HealthTextures.remember(id, health, base);
    }
}
