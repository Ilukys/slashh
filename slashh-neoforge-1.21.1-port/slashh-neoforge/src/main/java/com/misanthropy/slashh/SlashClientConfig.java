package com.misanthropy.slashh;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Slashh.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class SlashClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SHOW = BUILDER
            .comment("Draw slashes at all.").define("show", true);
    public static final ModConfigSpec.BooleanValue CLASSIC = BUILDER
            .comment("Use the old pixel cut instead of the smooth one.").define("classicCuts", true);
    public static final ModConfigSpec.DoubleValue SIZE = BUILDER
            .comment("Slash size.").defineInRange("size", 1.0, 0.4, 2.5);
    public static final ModConfigSpec.DoubleValue OPACITY = BUILDER
            .comment("Slash opacity.").defineInRange("opacity", 1.0, 0.1, 1.0);
    public static final ModConfigSpec.BooleanValue OTHERS = BUILDER
            .comment("Draw other players' slashes.").define("others", true);
    public static final ModConfigSpec.BooleanValue COLOURS = BUILDER
            .comment("Colour slashes by the damage type of the hit.").define("typeColours", true);
    public static final ModConfigSpec.BooleanValue FINISHERS = BUILDER
            .comment("Draw combo finishers as an X-cut.").define("finishers", true);
    public static final ModConfigSpec.BooleanValue SWEEPS = BUILDER
            .comment("Draw wide arcs on the extra targets of a sweep.").define("sweeps", true);
    public static final ModConfigSpec.BooleanValue SPRAY = BUILDER
            .comment("Blood spray on blood hits.").define("spray", true);
    public static final ModConfigSpec.DoubleValue SHAKE = BUILDER
            .comment("Camera shake on your own crits and finishers. 0 is off.").defineInRange("shake", 0.5, 0.0, 2.0);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static volatile boolean show = true;
    public static volatile boolean classic = false;
    public static volatile float size = 1.0f;
    public static volatile float opacity = 1.0f;
    public static volatile boolean others = true;
    public static volatile boolean colours = true;
    public static volatile boolean finishers = true;
    public static volatile boolean sweeps = true;
    public static volatile boolean spray = true;
    public static volatile float shake = 0.5f;

    private SlashClientConfig() {}

    public static void bake() {
        show = SHOW.get();
        classic = CLASSIC.get();
        size = SIZE.get().floatValue();
        opacity = OPACITY.get().floatValue();
        others = OTHERS.get();
        colours = COLOURS.get();
        finishers = FINISHERS.get();
        sweeps = SWEEPS.get();
        spray = SPRAY.get();
        shake = SHAKE.get().floatValue();
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
