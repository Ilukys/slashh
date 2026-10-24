package com.misanthropy.slashh.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class SlashNetwork {
    private static final String PROTOCOL_VERSION = "5";

    private SlashNetwork() {}

    /** Optional channel: a client with the mod can join a server without it, and the reverse. */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();
        registrar.playToClient(SlashEffectPacket.TYPE, SlashEffectPacket.STREAM_CODEC, ClientPacketHandler::handle);
    }
}
