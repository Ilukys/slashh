package com.misanthropy.slashh.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** The "classic" flat pixel cut, drawn as a camera-facing quad that can be rolled and stretched. */
public class SlashParticle extends TextureSheetParticle {
    private static final int FULL_BRIGHT = 15728880;

    private final SpriteSet sprites;
    private final float baseSize;
    private final float aspect;
    private final float fullAlpha;
    private final int delay;
    private final float grow;

    public SlashParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites,
                         float rollDeg, float baseSize, float aspect, int rgb, float fullAlpha,
                         int delay, int lifetime, float grow) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.baseSize = baseSize;
        this.aspect = aspect;
        this.fullAlpha = fullAlpha;
        this.delay = delay;
        this.grow = grow;
        this.lifetime = lifetime + delay;
        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.xd = this.yd = this.zd = 0.0;
        this.roll = this.oRoll = (float) Math.toRadians(rollDeg);
        this.quadSize = baseSize;
        this.alpha = delay > 0 ? 0.0f : fullAlpha;
        if (rgb >= 0) {
            this.setColor(((rgb >> 16) & 255) / 255.0f, ((rgb >> 8) & 255) / 255.0f, (rgb & 255) / 255.0f);
        }
        this.setSprite(sprites.get(0, lifetime));
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        int t = this.age - this.delay;
        if (t < 0) return;
        this.alpha = this.fullAlpha;
        this.setSprite(this.sprites.get(t, this.lifetime - this.delay));
    }

    @Override
    public float getQuadSize(float partialTick) {
        float s = Mth.clamp((this.age - this.delay + partialTick) / (this.lifetime - this.delay), 0.0f, 1.0f);
        return this.baseSize * (1.0f - this.grow * 0.5f + this.grow * s);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        if (this.alpha <= 0.0f) return;
        Vec3 cam = camera.getPosition();
        float fx = (float) (Mth.lerp((double) partialTick, this.xo, this.x) - cam.x());
        float fy = (float) (Mth.lerp((double) partialTick, this.yo, this.y) - cam.y());
        float fz = (float) (Mth.lerp((double) partialTick, this.zo, this.z) - cam.z());
        Quaternionf rot = new Quaternionf(camera.rotation()).rotateZ(Mth.lerp(partialTick, this.oRoll, this.roll));
        float size = this.getQuadSize(partialTick);

        Vector3f[] corners = {
                new Vector3f(-this.aspect, -1.0f, 0.0f),
                new Vector3f(-this.aspect, 1.0f, 0.0f),
                new Vector3f(this.aspect, 1.0f, 0.0f),
                new Vector3f(this.aspect, -1.0f, 0.0f)
        };
        for (Vector3f c : corners) {
            c.rotate(rot).mul(size).add(fx, fy, fz);
        }
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();

        vertex(buffer, corners[0], u1, v1);
        vertex(buffer, corners[1], u1, v0);
        vertex(buffer, corners[2], u0, v0);
        vertex(buffer, corners[3], u0, v1);
    }

    private void vertex(VertexConsumer buffer, Vector3f p, float u, float v) {
        buffer.addVertex(p.x(), p.y(), p.z())
                .setUv(u, v)
                .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
                .setLight(FULL_BRIGHT);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
