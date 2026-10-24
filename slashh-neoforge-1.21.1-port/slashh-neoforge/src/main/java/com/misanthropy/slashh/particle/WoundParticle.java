package com.misanthropy.slashh.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The smooth cut: a curved ribbon built from {@value #SEGMENTS} segments along a bent spine, drawn in three
 * stacked layers (a dark rim, a bright body and a pale core). It carves open over {@link #CARVE} ticks,
 * holds, then fades. Optionally anchored to an entity so it follows a moving target.
 */
public final class WoundParticle extends TextureSheetParticle {
    private static final int SEGMENTS = 24;
    private static final int LIGHT = 15728880;
    private static final float PEAK = 0.88f;
    private static final double BEND = 0.07;
    private static final double CARVE = 1.4;
    private static final double HOLD = 6.0;
    private static final double FADE = 4.0;

    /** Per layer: {width multiplier, alpha multiplier, how far the tint is washed toward white}. */
    private static final float[][] LAYERS = {
            {2.3f, 0.38f, 0.0f},
            {1.0f, 0.95f, 0.0f},
            {0.42f, 1.0f, 0.9f}
    };

    private final Entity anchor;
    private final Vec3 offset;
    private final Vec3 dir;
    private final Vec3 bulge;
    private final double length;
    private final double width;
    private final int delay;
    private final float strength;
    private final boolean jitter;
    private final float[][] tint;
    private final Vec3[] spine = new Vec3[SEGMENTS + 1];
    private final Vec3[] across = new Vec3[SEGMENTS + 1];

    public WoundParticle(ClientLevel level, SpriteSet sprites, Entity anchor, Vec3 pos, Vec3 direction,
                         Vec3 reference, double length, boolean wide, boolean jitter, int delay, int rgb,
                         float strength) {
        super(level, pos.x, pos.y, pos.z);
        this.anchor = anchor;
        this.offset = anchor == null ? pos : pos.subtract(anchor.position());
        this.dir = direction.normalize();
        Vec3 perp = reference.subtract(this.dir.scale(reference.dot(this.dir)));
        this.bulge = perp.lengthSqr() < 1.0E-8 ? Vec3.ZERO : perp.normalize();
        this.length = length;
        this.width = length * (wide ? 0.035 : 0.0275);
        this.delay = delay;
        this.strength = strength;
        this.jitter = jitter;

        float r = ((rgb >> 16) & 255) / 255.0f;
        float g = ((rgb >> 8) & 255) / 255.0f;
        float b = (rgb & 255) / 255.0f;
        this.tint = new float[LAYERS.length][];
        for (int i = 0; i < LAYERS.length; i++) {
            float wash = LAYERS[i][2];
            this.tint[i] = new float[]{Mth.lerp(wash, r, 1.0f), Mth.lerp(wash, g, 1.0f), Mth.lerp(wash, b, 1.0f)};
        }

        this.lifetime = delay + (int) Math.ceil(10.0) + 1;
        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        if (this.age++ >= this.lifetime || (this.anchor != null && this.anchor.isRemoved())) {
            this.remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        double t = this.age + partialTick - this.delay;
        if (t < 0.0) return;

        double carve = ease(t / CARVE);
        double fade = t < HOLD ? 1.0 : 1.0 - (t - HOLD) / FADE;
        double pulse = t > 4.0 ? 0.85 + 0.15 * Math.sin(t * 9.0) : 1.0;
        float alpha = (float) (Mth.clamp(fade, 0.0, 1.0) * pulse) * this.strength;
        if (carve < 0.004 || alpha <= 0.0f) return;

        Vec3 base = this.anchor == null ? this.offset : this.anchor.getPosition(partialTick).add(this.offset);
        Vec3 rel = base.subtract(camera.getPosition());
        double halfWidth = this.width * Mth.lerp(Mth.clamp(t - 1.5, 0.0, 1.0), 1.0, 0.55);

        for (int i = 0; i <= SEGMENTS; i++) {
            double s = (double) i / SEGMENTS * carve - 0.5;
            this.spine[i] = rel.add(this.dir.scale(s * this.length))
                    .add(this.bulge.scale(BEND * (1.0 - 4.0 * s * s) * this.length));
        }
        for (int i = 0; i <= SEGMENTS; i++) {
            Vec3 tangent = this.spine[Math.min(SEGMENTS, i + 1)].subtract(this.spine[Math.max(0, i - 1)]);
            Vec3 side = tangent.cross(this.spine[i]);
            this.across[i] = side.lengthSqr() < 1.0E-12 ? this.bulge : side.normalize();
        }

        for (int layer = 0; layer < LAYERS.length; layer++) {
            double lift = 1.0 - 0.0015 * (layer + 1);
            float layerAlpha = alpha * LAYERS[layer][1];
            float[] color = this.tint[layer];
            for (int j = 0; j < SEGMENTS; j++) {
                double a0 = (double) j / SEGMENTS;
                double a1 = (double) (j + 1) / SEGMENTS;
                Vec3 p0 = this.spine[j].add(this.across[j].scale(shake(j, t, halfWidth))).scale(lift);
                Vec3 p1 = this.spine[j + 1].add(this.across[j + 1].scale(shake(j + 1, t, halfWidth))).scale(lift);
                Vec3 w0 = this.across[j].scale(halfWidth * LAYERS[layer][0] * Math.pow(Math.sin(Math.PI * a0), 0.7));
                Vec3 w1 = this.across[j + 1].scale(halfWidth * LAYERS[layer][0] * Math.pow(Math.sin(Math.PI * a1), 0.7));
                float u0 = Mth.lerp((float) a0, this.getU0(), this.getU1());
                float u1 = Mth.lerp((float) a1, this.getU0(), this.getU1());
                quad(buffer, p0.subtract(w0), p0, p1, p1.subtract(w1), u0, u1, 0.0f, PEAK, color, layerAlpha);
                quad(buffer, p0, p0.add(w0), p1.add(w1), p1, u0, u1, PEAK, 0.0f, color, layerAlpha);
            }
        }
    }

    private double shake(int index, double t, double amplitude) {
        return this.jitter ? Math.sin(index * 12.9 + t * 7.1) * 0.6 * amplitude : 0.0;
    }

    /** Emits a quad twice, once per winding, so it shows from both sides. */
    private void quad(VertexConsumer buffer, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                      float u0, float u1, float vA, float vB, float[] color, float alpha) {
        float v0 = Mth.lerp(vA, this.getV0(), this.getV1());
        float v1 = Mth.lerp(vB, this.getV0(), this.getV1());
        vertex(buffer, a, u0, v0, color, alpha);
        vertex(buffer, b, u0, v1, color, alpha);
        vertex(buffer, c, u1, v1, color, alpha);
        vertex(buffer, d, u1, v0, color, alpha);
        vertex(buffer, d, u1, v0, color, alpha);
        vertex(buffer, c, u1, v1, color, alpha);
        vertex(buffer, b, u0, v1, color, alpha);
        vertex(buffer, a, u0, v0, color, alpha);
    }

    private static void vertex(VertexConsumer buffer, Vec3 p, float u, float v, float[] color, float alpha) {
        buffer.addVertex((float) p.x, (float) p.y, (float) p.z)
                .setUv(u, v)
                .setColor(color[0], color[1], color[2], alpha)
                .setLight(LIGHT);
    }

    private static double ease(double x) {
        double inv = 1.0 - Mth.clamp(x, 0.0, 1.0);
        return 1.0 - inv * inv * inv;
    }

    @Override
    public boolean shouldCull() {
        return false;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
