package com.misanthropy.slashh.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * What Better Combat says about the swing in progress: which animation, which hand, whether it ends the combo and
 * how long the weapon is. Better Combat is reached by reflection, so the mod needs no compile-time dependency on
 * it and degrades to "no shape" if its API ever changes.
 */
public record SwingShape(String animation, boolean offHand, boolean last, float length) {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean BETTER_COMBAT = ModList.get().isLoaded("bettercombat");

    private static final Set<String> LONG_BLADES =
            Set.of("claymore", "greatsword", "katana", "warglaive", "twin_blade", "coral_blade");
    private static final Set<String> HEADS = Set.of("halberd", "glaive", "spear", "lance", "trident", "scythe",
            "sickle", "greataxe", "heavy_axe", "double_axe", "axe", "greathammer", "hammer", "mace", "anchor",
            "battlestaff", "quarterstaff", "staff", "pickaxe");
    private static final Set<String> SMALL =
            Set.of("dagger", "sai", "claw", "fist", "soul_knife", "rapier", "sickle");

    public static SwingShape of(Player player) {
        return BETTER_COMBAT ? Bridge.of(player) : null;
    }

    static float length(String category, boolean twoHanded) {
        String c = category == null ? "" : category;
        if (SMALL.contains(c)) return 0.75f;
        if (LONG_BLADES.contains(c)) return 1.6f;
        if (HEADS.contains(c)) return 1.3f;
        return twoHanded ? 1.5f : 1.0f;
    }

    /** Reflection onto net.bettercombat.api.*; every handle is resolved once and the bridge disables itself on failure. */
    private static final class Bridge {
        private static boolean broken;
        private static Class<?> playerApi;
        private static Method getCurrentAttack;
        private static Method handAttack, handCombo, handIsOffHand, handAttributes;
        private static Method attackAnimation;
        private static Method comboCurrent, comboTotal;
        private static Method attrCategory, attrTwoHanded;

        private static void init() throws ReflectiveOperationException {
            playerApi = Class.forName("net.bettercombat.api.EntityPlayer_BetterCombat");
            getCurrentAttack = playerApi.getMethod("getCurrentAttack");
            Class<?> hand = Class.forName("net.bettercombat.api.AttackHand");
            handAttack = hand.getMethod("attack");
            handCombo = hand.getMethod("combo");
            handIsOffHand = hand.getMethod("isOffHand");
            handAttributes = hand.getMethod("attributes");
            attackAnimation = Class.forName("net.bettercombat.api.WeaponAttributes$Attack").getMethod("animation");
            Class<?> combo = Class.forName("net.bettercombat.api.ComboState");
            comboCurrent = combo.getMethod("current");
            comboTotal = combo.getMethod("total");
            Class<?> attrs = Class.forName("net.bettercombat.api.WeaponAttributes");
            attrCategory = attrs.getMethod("category");
            attrTwoHanded = attrs.getMethod("isTwoHanded");
        }

        static SwingShape of(Player player) {
            if (broken) return null;
            try {
                if (playerApi == null) init();
                if (!playerApi.isInstance(player)) return null;
                Object hand = getCurrentAttack.invoke(player);
                if (hand == null) return null;
                Object attack = handAttack.invoke(hand);
                if (attack == null) return null;
                String animation = (String) attackAnimation.invoke(attack);
                if (animation == null) return null;

                Object combo = handCombo.invoke(hand);
                boolean last = false;
                if (combo != null) {
                    int total = (int) comboTotal.invoke(combo);
                    last = total > 1 && (int) comboCurrent.invoke(combo) == total;
                }
                boolean offHand = (boolean) handIsOffHand.invoke(hand);
                Object attrs = handAttributes.invoke(hand);
                float length = 1.0f;
                if (attrs != null) {
                    length = SwingShape.length((String) attrCategory.invoke(attrs), (boolean) attrTwoHanded.invoke(attrs));
                }
                return new SwingShape(animation, offHand, last, length);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                broken = true;
                LOGGER.warn("Better Combat is loaded but its API did not match what Slashh expects; "
                        + "falling back to plain slashes.", e);
                return null;
            }
        }
    }
}
