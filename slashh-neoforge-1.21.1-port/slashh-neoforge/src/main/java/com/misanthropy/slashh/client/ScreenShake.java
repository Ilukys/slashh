package com.misanthropy.slashh.client;

import com.misanthropy.slashh.Slashh;
import com.misanthropy.slashh.SlashClientConfig;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Trauma-style camera shake: hits add trauma, it decays each tick, and the camera wobbles by trauma squared. */
@EventBusSubscriber(modid = Slashh.MODID, value = Dist.CLIENT)
public final class ScreenShake {
    private static final float MAX_DEGREES = 2.2f;

    private static float trauma;
    private static float prevTrauma;
    private static int ticks;

    private ScreenShake() {}

    public static void add(float amount) {
        if (SlashClientConfig.shake <= 0.0f) return;
        trauma = Math.min(1.0f, trauma + amount);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        prevTrauma = trauma;
        trauma = Math.max(0.0f, trauma - 0.12f);
        ticks++;
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        float t = Mth.lerp((float) event.getPartialTick(), prevTrauma, trauma);
        if (t <= 0.0f) return;
        float amplitude = t * t * MAX_DEGREES * SlashClientConfig.shake;
        float time = (ticks + (float) event.getPartialTick()) * 1.7f;
        event.setPitch(event.getPitch() + amplitude * Mth.sin(time * 3.1f));
        event.setYaw(event.getYaw() + amplitude * 0.7f * Mth.sin(time * 2.3f + 1.3f));
        event.setRoll(event.getRoll() + amplitude * 0.8f * Mth.sin(time * 3.7f + 2.1f));
    }
}
