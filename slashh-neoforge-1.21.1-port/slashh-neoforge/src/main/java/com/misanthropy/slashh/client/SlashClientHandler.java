package com.misanthropy.slashh.client;

import com.misanthropy.slashh.SlashClientConfig;
import com.misanthropy.slashh.SlashConfig;
import com.misanthropy.slashh.network.SlashEffectPacket;
import com.misanthropy.slashh.particle.DropletParticle;
import com.misanthropy.slashh.particle.SlashParticle;
import com.misanthropy.slashh.particle.WoundParticle;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Client side: turns a {@link SlashEffectPacket} into particles. */
public final class SlashClientHandler {
    private static final int BLOOD = 11538464;
    private static final int PLAIN = 14477048;
    private static final int CRIT = 16760944;
    private static final int LIGHTNING = 16769357;

    private static long capTick = Long.MIN_VALUE;
    private static int spawnedThisTick;

    private SlashClientHandler() {}

    /** One swing's arc: a circle of {@code radius} around {@code center}, in the plane spanned by forward and side. */
    private record Arc(Vec3 center, Vec3 forward, Vec3 side, Vec3 normal, double radius, double hit, double lift) {
        /** The arc that passes through {@code through}, with the cut landing at {@code aim}. */
        static Arc through(Vec3 center, Vec3 forward, Vec3 side, Vec3 aim) {
            Vec3 normal = forward.cross(side).normalize();
            Vec3 toAim = aim.subtract(center);
            double lift = toAim.dot(normal);
            Vec3 flat = toAim.subtract(normal.scale(lift));
            return new Arc(center, forward, side, normal,
                    Math.max(0.6, flat.length()),
                    Math.atan2(flat.dot(side), flat.dot(forward)),
                    lift);
        }

        Vec3 at(double angle) {
            return center.add(forward.scale(Math.cos(angle) * radius))
                    .add(side.scale(Math.sin(angle) * radius))
                    .add(normal.scale(lift));
        }

        /** Direction the blade is moving at the hit point. */
        Vec3 travel() {
            return forward.scale(Math.sin(hit)).subtract(side.scale(Math.cos(hit)));
        }

        /** Direction pointing away from the arc's centre at the hit point. */
        Vec3 outward() {
            return forward.scale(Math.cos(hit)).add(side.scale(Math.sin(hit)));
        }
    }

    private static boolean ready(ClientLevel level) {
        if (!SlashClientConfig.show || level == null || SlashClientEvents.slash == null) return false;
        long now = level.getGameTime();
        if (now != capTick) {
            capTick = now;
            spawnedThisTick = 0;
        }
        if (spawnedThisTick >= SlashConfig.maxEffectsPerTick) return false;
        spawnedThisTick++;
        return true;
    }

    private static boolean mine(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getId() == entityId;
    }

    private static void add(Particle particle) {
        Minecraft.getInstance().particleEngine.add(particle);
    }

    public static void slash(SlashEffectPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        boolean mine = mine(packet.attacker());
        if (!mine && !SlashClientConfig.others) return;
        if (!ready(level)) return;

        boolean crit = packet.has(SlashEffectPacket.CRIT);
        int rgb = SlashClientConfig.colours ? packet.rgb() : -1;
        if (crit && rgb >= 0) rgb = brighten(rgb);

        List<SwingSpecs.Spec> specs = packet.animation().isEmpty()
                ? List.of(SwingSpecs.arc(packet.rollDeg()))
                : SwingSpecs.specs(packet.animation());
        if (specs.isEmpty()) return;

        if (specs.stream().allMatch(SwingSpecs.Spec::stab)) {
            if (SlashClientConfig.classic || SlashClientEvents.wound == null) {
                stab(level, packet, rgb, crit);
            } else {
                pierce(level, packet, rgb, crit);
            }
            if (crit && mine) ScreenShake.add(0.25f);
            return;
        }

        Vec3 pos = new Vec3(packet.x(), packet.y(), packet.z());
        Vec3 dir = new Vec3(packet.dx(), packet.dy(), packet.dz());
        Entity attacker = level.getEntity(packet.attacker());
        boolean attackerLeft = attacker instanceof LivingEntity living && living.getMainArm() == HumanoidArm.LEFT;
        boolean left = packet.has(SlashEffectPacket.OFFHAND) != attackerLeft;
        Vec3 eye = attacker != null ? attacker.getEyePosition() : pos.subtract(dir.scale(2.5));

        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) Math.toDegrees(-Math.asin(Mth.clamp(dir.y, -1.0, 1.0)));
        Vec3 side = Vec3.directionFromRotation(0.0f, yaw + 90.0f);

        boolean sweep = packet.has(SlashEffectPacket.SWEEP) && SlashClientConfig.sweeps;
        float size = packet.size() * SlashClientConfig.size * (sweep ? 1.25f : 1.0f);

        double avgX = specs.stream().filter(s -> !s.stab()).mapToDouble(SwingSpecs.Spec::x).average().orElse(0.0);
        double avgY = specs.stream().filter(s -> !s.stab()).mapToDouble(SwingSpecs.Spec::y).average().orElse(0.0);

        boolean finisher = packet.has(SlashEffectPacket.FINISHER) && SlashClientConfig.finishers;
        for (SwingSpecs.Spec spec : specs) {
            if (spec.stab()) continue;
            Vec3 aim = pos.add(side.scale((left ? -1.0 : 1.0) * (spec.x() - avgX))).add(0.0, spec.y() - avgY, 0.0);
            spawn(level, packet, arc(spec, spec.tilt(), eye, yaw, pitch, left, aim), size, 0, rgb, crit, sweep);
            if (finisher) {
                spawn(level, packet, arc(spec, spec.tilt() + 90.0, eye, yaw, pitch, left, aim),
                        size * 1.2f, 2, rgb, crit, false);
                finisher = false;
            }
        }

        if (packet.has(SlashEffectPacket.FINISHER) && SlashClientConfig.finishers) {
            if (mine) ScreenShake.add(0.45f);
        } else if (crit && mine) {
            ScreenShake.add(0.25f);
        }

        if (packet.has(SlashEffectPacket.SPRAY) && SlashClientConfig.spray) {
            spray(level, packet.x(), packet.y(), packet.z(), dir, packet.rgb() >= 0 ? packet.rgb() : BLOOD, crit ? 12 : 7);
        }
    }

    private static Arc arc(SwingSpecs.Spec spec, double tilt, Vec3 origin, float yaw, float pitch,
                           boolean left, Vec3 aim) {
        double angle = Math.toRadians(left ? 180.0 - tilt : tilt);
        Vec3 side = Vec3.directionFromRotation(0.0f, yaw + 90.0f);
        Vec3 forward = Vec3.directionFromRotation(pitch + (float) spec.pitch(), yaw);
        Vec3 normal = side.cross(forward);
        Vec3 ring = side.scale(Math.cos(angle)).subtract(normal.scale(Math.sin(angle)));
        Vec3 center = new Vec3(origin.x, origin.y - 0.25 + spec.y(), origin.z)
                .add(forward.scale(spec.z()))
                .add(side.scale(left ? -spec.x() : spec.x()));
        return Arc.through(center, forward, ring, aim);
    }

    private static void spawn(ClientLevel level, SlashEffectPacket packet, Arc arc, float size, int delay,
                              int rgb, boolean crit, boolean sweep) {
        float alpha = SlashClientConfig.opacity * (sweep ? 0.8f : 1.0f);
        Vec3 hit = arc.at(arc.hit());

        if (SlashClientConfig.classic) {
            SpriteSet sprites = crit && rgb < 0 ? SlashClientEvents.crit : SlashClientEvents.slash;
            add(new SlashParticle(level, hit.x, hit.y, hit.z, sprites, classicRoll(arc), size * 0.5f,
                    sweep ? 1.3f : 1.0f, rgb, alpha, delay, 10, 0.2f));
        } else {
            wound(level, packet, hit, arc.travel(), arc.outward(), size, delay, rgb, crit, alpha);
        }
    }

    private static void wound(ClientLevel level, SlashEffectPacket packet, Vec3 pos, Vec3 travel, Vec3 outward,
                              float size, int delay, int rgb, boolean crit, float alpha) {
        if (SlashClientEvents.wound == null) return;
        add(new WoundParticle(level, SlashClientEvents.wound, level.getEntity(packet.target()), pos, travel, outward,
                size, crit, packet.rgb() == LIGHTNING, delay,
                rgb >= 0 ? rgb : crit ? CRIT : PLAIN, alpha));
    }

    /** Angle of a world-space direction as seen on screen, in degrees. */
    private static float screenAngle(Vec3 v) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        double x = v.dot(new Vec3(camera.getLeftVector()));
        double y = v.dot(new Vec3(camera.getUpVector()));
        return x * x + y * y < 1.0E-4 ? 0.0f : (float) Math.toDegrees(Math.atan2(y, x));
    }

    private static float classicRoll(Arc arc) {
        return screenAngle(arc.travel()) - 135.0f;
    }

    private static void pierce(ClientLevel level, SlashEffectPacket packet, int rgb, boolean crit) {
        Vec3 pos = new Vec3(packet.x(), packet.y(), packet.z());
        Vec3 dir = new Vec3(packet.dx(), packet.dy(), packet.dz());
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 tangent = up.subtract(dir.scale(up.dot(dir) / Math.max(1.0E-6, dir.lengthSqr())));
        if (tangent.lengthSqr() < 1.0E-6) tangent = new Vec3(1.0, 0.0, 0.0);
        wound(level, packet, pos, tangent, dir.cross(tangent),
                packet.size() * SlashClientConfig.size * 0.6f, 0, rgb, crit, SlashClientConfig.opacity);
        if (packet.has(SlashEffectPacket.SPRAY) && SlashClientConfig.spray) {
            spray(level, packet.x(), packet.y(), packet.z(), dir, packet.rgb() >= 0 ? packet.rgb() : BLOOD, crit ? 10 : 6);
        }
    }

    private static void stab(ClientLevel level, SlashEffectPacket packet, int rgb, boolean crit) {
        RandomSource random = level.getRandom();
        Vec3 dir = new Vec3(packet.dx(), packet.dy(), packet.dz());
        int count = crit ? 12 : 7;
        for (int i = 0; i < count; i++) {
            Vec3 v = dir.scale(0.25 + random.nextDouble() * 0.35)
                    .add(random.nextGaussian() * 0.08, random.nextGaussian() * 0.08, random.nextGaussian() * 0.08);
            level.addParticle(crit ? STAB_CRIT : STAB, packet.x(), packet.y(), packet.z(), v.x, v.y, v.z);
        }
        if (packet.has(SlashEffectPacket.SPRAY) && SlashClientConfig.spray) {
            spray(level, packet.x(), packet.y(), packet.z(), dir, packet.rgb() >= 0 ? packet.rgb() : BLOOD, crit ? 10 : 6);
        }
    }

    // The 1.20.1 jar referenced these by obfuscated field name only; see the porting notes before trusting them.
    private static final net.minecraft.core.particles.SimpleParticleType STAB = ParticleTypes.CRIT;
    private static final net.minecraft.core.particles.SimpleParticleType STAB_CRIT = ParticleTypes.ENCHANTED_HIT;

    private static void spray(ClientLevel level, double x, double y, double z, Vec3 dir, int rgb, int count) {
        if (SlashClientEvents.droplet == null) return;
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; i++) {
            double speed = 0.12 + random.nextDouble() * 0.18;
            Vec3 v = dir.scale(speed * 0.8).add(
                    random.nextGaussian() * 0.09,
                    0.08 + random.nextDouble() * 0.16,
                    random.nextGaussian() * 0.09);
            add(new DropletParticle(level, x, y, z, v.x, v.y, v.z, SlashClientEvents.droplet, rgb,
                    0.035f + random.nextFloat() * 0.035f));
        }
    }

    /** Pulls a colour a third of the way toward white, for crits. */
    private static int brighten(int rgb) {
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        return ((r + (255 - r) / 3) << 16) | ((g + (255 - g) / 3) << 8) | (b + (255 - b) / 3);
    }
}
