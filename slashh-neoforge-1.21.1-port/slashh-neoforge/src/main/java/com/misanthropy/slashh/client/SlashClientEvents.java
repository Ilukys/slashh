package com.misanthropy.slashh.client;

import com.misanthropy.slashh.Slashh;
import com.misanthropy.slashh.particle.ModParticles;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * Registers the particle sprite sets and the swing-spec reload listener. The particles are never spawned through
 * the vanilla path (the providers return null); {@link SlashClientHandler} builds them itself, so these providers
 * only exist to capture each type's sprite set.
 */
@EventBusSubscriber(modid = Slashh.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SlashClientEvents {
    static SpriteSet slash;
    static SpriteSet crit;
    static SpriteSet droplet;
    static SpriteSet wound;

    private SlashClientEvents() {}

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) SwingSpecs::reload);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SLASH.get(), sprites -> {
            slash = sprites;
            return (type, level, x, y, z, xd, yd, zd) -> null;
        });
        event.registerSpriteSet(ModParticles.SLASH_CRIT.get(), sprites -> {
            crit = sprites;
            return (type, level, x, y, z, xd, yd, zd) -> null;
        });
        event.registerSpriteSet(ModParticles.WOUND.get(), sprites -> {
            wound = sprites;
            return (type, level, x, y, z, xd, yd, zd) -> null;
        });
        event.registerSpriteSet(ModParticles.DROPLET.get(), sprites -> {
            droplet = sprites;
            return (type, level, x, y, z, xd, yd, zd) -> null;
        });
    }
}
