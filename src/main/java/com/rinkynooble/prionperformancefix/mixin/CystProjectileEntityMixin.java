package com.rinkynooble.prionperformancefix.mixin;

import net.demod.prionmod.entity.custom.CystProjectileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A cyst acts in its own dimension. Prion's {@code onHitBlock} takes the Overworld from the server
 * for its particles, its living blocks and its Lure, whatever dimension the cyst is in, so a cyst
 * elsewhere changed the Overworld at the same x and z (loading that chunk if it wasn't).
 */
@Mixin(CystProjectileEntity.class)
public abstract class CystProjectileEntityMixin {
    @Redirect(method = {"onHitBlock", "m_8060_"}, remap = false,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;overworld()Lnet/minecraft/server/level/ServerLevel;", remap = true))
    private ServerLevel prionperformancefix$ownLevel(MinecraftServer server) {
        return ((Entity) (Object) this).level() instanceof ServerLevel own ? own : server.overworld();
    }
}
