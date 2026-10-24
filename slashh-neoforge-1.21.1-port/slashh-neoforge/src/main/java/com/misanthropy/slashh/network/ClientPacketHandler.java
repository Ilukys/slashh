package com.misanthropy.slashh.network;

import com.misanthropy.slashh.client.SlashClientHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Only ever invoked on the client (playToClient), so touching client classes here is safe. */
final class ClientPacketHandler {
    private ClientPacketHandler() {}

    static void handle(SlashEffectPacket packet, IPayloadContext context) {
        SlashClientHandler.slash(packet);
    }
}
