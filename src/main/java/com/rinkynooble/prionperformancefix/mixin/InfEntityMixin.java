package com.rinkynooble.prionperformancefix.mixin;

import net.demod.prionmod.entity.custom.InfEntity;
import net.demod.prionmod.world.gen.MobCapConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prion's spawn check asks the kind's cap first. Prion scans every block within 14 of the mob for a
 * light source (about 28,800 blocks when there's none) and only then checks the cap, a map lookup, so
 * a kind already at its cap paid for the whole scan on every spawn attempt. Both checks only read, so
 * asking the cap first gives the same answer; with room under the cap, Prion's own check runs as
 * before.
 */
@Mixin(InfEntity.class)
public abstract class InfEntityMixin {
    @Inject(method = {"checkSpawnRules", "m_5545_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void prionperformancefix$capFirst(LevelAccessor level, MobSpawnType reason, CallbackInfoReturnable<Boolean> cir) {
        MinecraftServer server = level.getServer();
        if (server != null && !MobCapConfig.canSpawn(((Entity) (Object) this).getType(), server.overworld())) {
            cir.setReturnValue(false);
        }
    }
}
