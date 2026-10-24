package com.misanthropy.slashh.client;

import com.misanthropy.slashh.SlashClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The "Config" screen for Slashh in the mod list. Every control writes the client config and re-bakes it at once. */
public class SlashSettingsScreen extends Screen {
    private static final int W = 150;
    private static final int H = 20;
    private static final int GAP = 4;

    private final Screen parent;

    public SlashSettingsScreen(Screen parent) {
        super(Component.translatable("slashh.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - W - GAP / 2;
        int right = this.width / 2 + GAP / 2;
        int y = 40;

        addRenderableWidget(toggle(left, y, "show", SlashClientConfig.SHOW));
        addRenderableWidget(toggle(right, y, "others", SlashClientConfig.OTHERS));
        y += 24;
        addRenderableWidget(new Slider(left, y, "size", SlashClientConfig.SIZE, 0.4, 2.5));
        addRenderableWidget(new Slider(right, y, "opacity", SlashClientConfig.OPACITY, 0.1, 1.0));
        y += 24;
        addRenderableWidget(toggle(left, y, "colours", SlashClientConfig.COLOURS));
        addRenderableWidget(toggle(right, y, "finishers", SlashClientConfig.FINISHERS));
        y += 24;
        addRenderableWidget(toggle(left, y, "sweeps", SlashClientConfig.SWEEPS));
        addRenderableWidget(toggle(right, y, "spray", SlashClientConfig.SPRAY));
        y += 24;
        addRenderableWidget(new Slider(left, y, "shake", SlashClientConfig.SHAKE, 0.0, 2.0));
        addRenderableWidget(toggle(right, y, "classic", SlashClientConfig.CLASSIC));
        y += 36;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(this.width / 2 - 100, y, 200, H).build());
    }

    private static CycleButton<Boolean> toggle(int x, int y, String key, ModConfigSpec.BooleanValue value) {
        return CycleButton.onOffBuilder(value.get())
                .withTooltip(on -> Tooltip.create(Component.translatable("slashh.settings." + key + ".tip")))
                .create(x, y, W, H, Component.translatable("slashh.settings." + key), (button, on) -> {
                    value.set(on);
                    SlashClientConfig.bake();
                });
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Screen.render draws the background itself in 1.21; drawing it here too would blur twice and crash.
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        SlashClientConfig.SPEC.save();
        SlashClientConfig.bake();
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    /** A 0-100 % slider over [min, max], snapped to 5% steps; 0 reads as "OFF". */
    private static final class Slider extends AbstractSliderButton {
        private final String key;
        private final ModConfigSpec.DoubleValue config;
        private final double min;
        private final double max;

        Slider(int x, int y, String key, ModConfigSpec.DoubleValue config, double min, double max) {
            super(x, y, W, H, CommonComponents.EMPTY, (config.get() - min) / (max - min));
            this.key = key;
            this.config = config;
            this.min = min;
            this.max = max;
            updateMessage();
        }

        private double current() {
            return Math.round((min + this.value * (max - min)) * 20.0) / 20.0;
        }

        @Override
        protected void updateMessage() {
            double v = current();
            Component shown = v <= 0.0 ? CommonComponents.OPTION_OFF : Component.literal(Math.round(v * 100.0) + "%");
            setMessage(Component.translatable("slashh.settings." + key).append(": ").append(shown));
        }

        @Override
        protected void applyValue() {
            config.set(current());
            SlashClientConfig.bake();
        }
    }
}
