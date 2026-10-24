package com.misanthropy.slashh.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

public class DropletParticle extends TextureSheetParticle {
    public DropletParticle(ClientLevel level, double x, double y, double z,
                           double xd, double yd, double zd, SpriteSet sprites, int rgb, float size) {
        super(level, x, y, z);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.gravity = 0.9f;
        this.friction = 0.96f;
        this.hasPhysics = true;
        this.lifetime = 16 + this.random.nextInt(12);
        this.quadSize = size;
        float shade = 0.75f + this.random.nextFloat() * 0.25f;
        this.setColor(((rgb >> 16) & 255) / 255.0f * shade,
                ((rgb >> 8) & 255) / 255.0f * shade,
                (rgb & 255) / 255.0f * shade);
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.onGround) {
            this.xd = this.zd = 0.0;
            this.quadSize *= 0.9f;
        }
        int left = this.lifetime - this.age;
        if (left < 6) {
            this.alpha = left / 6.0f;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
