package com.rinkynooble.prionperformancefix.mixin.client;

import com.rinkynooble.prionperformancefix.client.AllParts;
import com.rinkynooble.prionperformancefix.client.BoneIndex;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Gives each model part slots for its {@link BoneIndex} and its {@link AllParts} list, each filled
 * the first time it's asked for, and answers {@code getAllParts} from that list.
 */
@Mixin(ModelPart.class)
public abstract class ModelPartMixin implements BoneIndex.Part {
    @Shadow
    @Final
    private Map<String, ModelPart> children;

    @Unique
    private Map<String, Optional<ModelPart>> prionperformancefix$boneIndex;

    @Unique
    private List<ModelPart> prionperformancefix$allParts;

    @Inject(method = "getAllParts", at = @At("HEAD"), cancellable = true)
    private void prionperformancefix$listedParts(CallbackInfoReturnable<Stream<ModelPart>> cir) {
        List<ModelPart> parts = AllParts.of((ModelPart) (Object) this);
        if (parts != null) {
            cir.setReturnValue(parts.stream());
        }
    }

    @Override
    public Map<String, ModelPart> prionperformancefix$children() {
        return this.children;
    }

    @Override
    public Map<String, Optional<ModelPart>> prionperformancefix$boneIndex() {
        return this.prionperformancefix$boneIndex;
    }

    @Override
    public void prionperformancefix$setBoneIndex(Map<String, Optional<ModelPart>> index) {
        this.prionperformancefix$boneIndex = index;
    }

    @Override
    public List<ModelPart> prionperformancefix$allParts() {
        return this.prionperformancefix$allParts;
    }

    @Override
    public void prionperformancefix$setAllParts(List<ModelPart> parts) {
        this.prionperformancefix$allParts = parts;
    }
}
