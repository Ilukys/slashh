package com.misanthropy.slashh.api;

import com.misanthropy.slashh.event.CombatEventHandler;
import net.minecraft.world.entity.LivingEntity;

/** Lets other mods tint (and optionally add blood spray to) the next slash on an entity, server side. */
public final class SlashhApi {
    private SlashhApi() {}

    public static void tint(LivingEntity entity, int rgb, boolean spray) {
        if (entity.level().isClientSide()) return;
        CombatEventHandler.tint(entity, rgb, spray);
    }
}
