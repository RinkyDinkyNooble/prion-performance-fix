package com.rinkynooble.prionperformancefix.mixin;

import com.rinkynooble.prionperformancefix.ProtectedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Every block Prion places goes through {@code setBlockAndUpdate} (cysts, Hive rings, a rooting
 * Hive's floor, entrails on a kill, eggs on an infected death), so one check here covers them all:
 * placing one of Prion's blocks leaves a {@link ProtectedBlocks protected} block as it is. Any other
 * block placed here passes straight through.
 */
@Mixin(Level.class)
public abstract class LevelMixin {
    @Inject(method = "setBlockAndUpdate", at = @At("HEAD"), cancellable = true)
    private void prionperformancefix$spareProtected(BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (ProtectedBlocks.isPrionBlock(state.getBlock()) && ProtectedBlocks.spares((Level) (Object) this, pos)) {
            cir.setReturnValue(false);
        }
    }
}
