package com.misanthropy.slashh.particle;

import com.misanthropy.slashh.Slashh;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Slashh.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SLASH =
            PARTICLE_TYPES.register("slash", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SLASH_CRIT =
            PARTICLE_TYPES.register("slash_crit", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WOUND =
            PARTICLE_TYPES.register("wound", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DROPLET =
            PARTICLE_TYPES.register("droplet", () -> new SimpleParticleType(false));

    private ModParticles() {}
}
