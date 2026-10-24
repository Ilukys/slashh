package com.misanthropy.slashh;

import com.misanthropy.slashh.client.SlashSettingsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only entrypoint: registers the in-game settings screen in the mod list. */
@Mod(value = Slashh.MODID, dist = Dist.CLIENT)
public class SlashhClient {
    public SlashhClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> new SlashSettingsScreen(parent));
    }
}
