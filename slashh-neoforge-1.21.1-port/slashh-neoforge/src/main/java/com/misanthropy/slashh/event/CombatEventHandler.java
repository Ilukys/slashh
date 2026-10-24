package com.misanthropy.slashh.event;

import com.misanthropy.slashh.Slashh;
import com.misanthropy.slashh.SlashConfig;
import com.misanthropy.slashh.compat.CritHelper;
import com.misanthropy.slashh.compat.SwingShape;
import com.misanthropy.slashh.network.SlashEffectPacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Server side: turns a player's melee hit into a {@link SlashEffectPacket} sent to everyone tracking the victim. */
@EventBusSubscriber(modid = Slashh.MODID)
public final class CombatEventHandler {
    public static final TagKey<Item> SLASHES = tag("slashes");
    public static final TagKey<Item> NO_SLASH = tag("no_slash");
    public static final TagKey<Item> HEAVY = tag("heavy");
    public static final TagKey<Item> QUICK = tag("quick");

    private static final float MAX_ROLL_DEGREES = 45.0f;

    private record Combo(int count, long last) {}

    private record Tint(int rgb, boolean spray, long tick) {}

    private static final Map<UUID, Combo> COMBOS = new HashMap<>();
    private static final Map<UUID, Integer> SENT_THIS_TICK = new HashMap<>();
    private static final Int2ObjectOpenHashMap<Tint> TINTS = new Int2ObjectOpenHashMap<>();
    private static long capTick = Long.MIN_VALUE;

    private CombatEventHandler() {}

    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Slashh.MODID, name));
    }

    public static boolean slashes(ItemStack stack) {
        return stack.is(SLASHES) && !stack.is(NO_SLASH);
    }

    /** Colours the next slash on this entity, if it lands this same tick. */
    public static void tint(LivingEntity entity, int rgb, boolean spray) {
        TINTS.put(entity.getId(), new Tint(rgb, spray, entity.level().getGameTime()));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        COMBOS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide()) return;

        long now = level.getGameTime();
        if (now != capTick) {
            capTick = now;
            SENT_THIS_TICK.clear();
            if (TINTS.size() > 256) TINTS.clear();
        }
        Tint tint = TINTS.remove(victim.getId());

        if (!SlashConfig.enabled) return;
        Entity source = event.getSource().getEntity();
        if (!(source instanceof Player player) || victim == player) return;

        ItemStack held = player.getMainHandItem();
        if (!slashes(held)) return;

        UUID id = player.getUUID();
        int sent = SENT_THIS_TICK.getOrDefault(id, 0);
        if (sent >= SlashConfig.maxEffectsPerTick) return;
        SENT_THIS_TICK.put(id, sent + 1);
        boolean extraTarget = sent > 0;

        Combo previous = COMBOS.get(id);
        int comboCount;
        if (extraTarget && previous != null) {
            comboCount = previous.count();
        } else if (previous != null && now - previous.last() <= SlashConfig.comboResetTicks) {
            comboCount = previous.count() + 1;
        } else {
            comboCount = 0;
        }
        COMBOS.put(id, new Combo(comboCount, now));

        RandomSource random = level.getRandom();
        boolean heavy = held.is(HEAVY);
        boolean quick = held.is(QUICK);
        SwingShape shape = SwingShape.of(player);

        float roll = 0.0f;
        if (shape == null) {
            float magnitude = heavy ? 10.0f + random.nextFloat() * 20.0f : 15.0f + random.nextFloat() * 30.0f;
            roll = Mth.clamp((comboCount % 2 == 0 ? 1.0f : -1.0f) * magnitude, -MAX_ROLL_DEGREES, MAX_ROLL_DEGREES);
        }

        float size = (shape != null ? shape.length() : heavy ? 1.4f : quick ? 0.75f : 1.0f)
                * damageScale(event.getNewDamage());

        int flags = 0;
        if (CritHelper.isCrit(player, victim)) {
            flags |= SlashEffectPacket.CRIT;
            size *= 1.15f;
        }

        int finisherEvery = SlashConfig.finisherEvery;
        boolean finisher = shape != null
                ? shape.last()
                : finisherEvery > 0 && comboCount > 0 && (comboCount + 1) % finisherEvery == 0;
        if (!extraTarget && finisherEvery > 0 && finisher) flags |= SlashEffectPacket.FINISHER;
        if (extraTarget) flags |= SlashEffectPacket.SWEEP;
        if (shape != null && shape.offHand()) flags |= SlashEffectPacket.OFFHAND;

        boolean tinted = tint != null && tint.tick() == now;
        int rgb = tinted ? tint.rgb() : -1;
        if (tinted && tint.spray()) flags |= SlashEffectPacket.SPRAY;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 impact = impact(victim, eye, look);

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(victim, new SlashEffectPacket(
                impact.x, impact.y, impact.z, roll, size, rgb, flags, player.getId(),
                (float) look.x, (float) look.y, (float) look.z,
                shape == null ? "" : shape.animation(), victim.getId()));
    }

    static float damageScale(float amount) {
        return Math.min(3.5f, 1.0f + 0.8f * (float) Math.pow(Math.max(0.0f, amount) / 100.0f, 1.3));
    }

    /** Where the cut shows: the point the player's look ray enters the victim, else a point on its near side. */
    private static Vec3 impact(LivingEntity victim, Vec3 eye, Vec3 look) {
        AABB box = victim.getBoundingBox().inflate(0.15);
        Optional<Vec3> hit = box.clip(eye, eye.add(look.scale(6.0)));
        if (hit.isPresent()) return hit.get();

        double y = Mth.clamp(eye.y, victim.getY(), victim.getY() + victim.getBbHeight());
        Vec3 center = new Vec3(victim.getX(), y, victim.getZ());
        Vec3 away = eye.subtract(center);
        away = away.lengthSqr() < 1.0E-4 ? look.scale(-1.0) : away.normalize();
        return center.add(away.scale(victim.getBbWidth() * 0.5 + 0.1));
    }
}
