package com.rinkynooble.prionperformancefix.client;

import com.rinkynooble.prionperformancefix.PrionPerformanceFix;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PrionPerformanceFix.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    /** A mob that leaves the client's world (dies, despawns or unloads) gives back its health texture. */
    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            HealthTextures.release(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            HealthTextures.releaseAll();
        }
    }
}
