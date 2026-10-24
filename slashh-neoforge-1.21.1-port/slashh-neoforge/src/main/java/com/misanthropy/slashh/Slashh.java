package com.misanthropy.slashh;

import com.misanthropy.slashh.network.SlashNetwork;
import com.misanthropy.slashh.particle.ModParticles;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(Slashh.MODID)
public class Slashh {
    public static final String MODID = "slashh";

    public Slashh(IEventBus modBus, ModContainer container) {
        modBus.addListener(SlashNetwork::register);
        ModParticles.PARTICLE_TYPES.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, SlashConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, SlashClientConfig.SPEC);
    }
}
