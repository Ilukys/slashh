package com.misanthropy.slashh;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Slashh.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class SlashConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Master switch. Off means the server sends no slashes at all. Players tune what they see in the client config",
                    "or the settings screen in the mod list.")
            .define("enabled", true);

    private static final ModConfigSpec.IntValue MAX_EFFECTS_PER_TICK = BUILDER
            .comment("Most slashes one attacker sends per tick, and most a client draws per tick. A sweep through 30 mobs",
                    "still costs at most this many.")
            .defineInRange("maxEffectsPerTick", 6, 1, 128);

    private static final ModConfigSpec.IntValue COMBO_RESET_TICKS = BUILDER
            .comment("Ticks between hits before a combo resets. Consecutive hits alternate direction and build to a finisher.")
            .defineInRange("comboResetTicks", 30, 5, 200);

    private static final ModConfigSpec.IntValue FINISHER_EVERY = BUILDER
            .comment("Every Nth hit of a combo is a finisher, a crossed X-cut. 0 turns finishers off for everyone.")
            .defineInRange("finisherEvery", 3, 0, 20);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static volatile boolean enabled = true;
    public static volatile int maxEffectsPerTick = 6;
    public static volatile int comboResetTicks = 30;
    public static volatile int finisherEvery = 3;

    private SlashConfig() {}

    private static void bake() {
        enabled = ENABLED.get();
        maxEffectsPerTick = MAX_EFFECTS_PER_TICK.get();
        comboResetTicks = COMBO_RESET_TICKS.get();
        finisherEvery = FINISHER_EVERY.get();
    }

    @SubscribeEvent
    static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) bake();
    }

    @SubscribeEvent
    static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) bake();
    }
}
