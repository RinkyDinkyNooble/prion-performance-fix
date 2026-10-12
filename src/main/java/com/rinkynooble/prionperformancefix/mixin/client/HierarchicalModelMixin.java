package com.rinkynooble.prionperformancefix.mixin.client;

import com.rinkynooble.prionperformancefix.client.BoneIndex;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Keyframe animations look up every animated bone by name on every frame. The lookup is answered
 * from {@link BoneIndex}. The {@code "root"} name, and trees the index doesn't support, keep
 * Minecraft's own code.
 *
 * <p>An injection rather than an overwrite, so it still applies on top of Embeddium's own version
 * of this method when that one is active (Embeddium without Oculus).
 */
@Mixin(HierarchicalModel.class)
public abstract class HierarchicalModelMixin {
    @Shadow
    public abstract ModelPart root();

    @Inject(method = "getAnyDescendantWithName", at = @At("HEAD"), cancellable = true)
    private void prionperformancefix$indexedLookup(String name, CallbackInfoReturnable<Optional<ModelPart>> cir) {
        if (name.equals("root")) {
            return;
        }
        ModelPart root = this.root();
        if (root != null) {
            Optional<ModelPart> found = BoneIndex.find(root, name);
            if (found != null) {
                cir.setReturnValue(found);
            }
        }
    }
}
