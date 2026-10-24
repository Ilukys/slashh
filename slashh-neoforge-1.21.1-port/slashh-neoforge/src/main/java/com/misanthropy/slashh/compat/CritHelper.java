package com.misanthropy.slashh.compat;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

/** Crit detection. Uses Better Combat Additions' jump-crit rule when present, vanilla's rule otherwise. */
public final class CritHelper {
    private static final String JUMP_CRIT = "com.misanthropy.linggango.bettercombatadditions.crit.JumpCrit";
    private static Boolean bcaAvailable;
    private static Method isJumpCrit;

    private CritHelper() {}

    public static boolean isCrit(Player player, LivingEntity target) {
        if (isBcaAvailable()) {
            try {
                return (boolean) isJumpCrit.invoke(null, player, target);
            } catch (ReflectiveOperationException | RuntimeException e) {
                bcaAvailable = false;
            }
        }
        return isVanillaCrit(player);
    }

    private static boolean isBcaAvailable() {
        if (bcaAvailable == null) {
            try {
                Class<?> type = Class.forName(JUMP_CRIT, false, CritHelper.class.getClassLoader());
                isJumpCrit = type.getMethod("isJumpCrit", Player.class, net.minecraft.world.entity.Entity.class);
                bcaAvailable = true;
            } catch (ReflectiveOperationException | LinkageError e) {
                bcaAvailable = false;
            }
        }
        return bcaAvailable;
    }

    public static boolean isVanillaCrit(Player player) {
        return player.fallDistance > 0.0f
                && !player.onGround()
                && !player.onClimbable()
                && !player.isInWater()
                && !player.hasEffect(MobEffects.BLINDNESS)
                && !player.isPassenger();
    }
}
