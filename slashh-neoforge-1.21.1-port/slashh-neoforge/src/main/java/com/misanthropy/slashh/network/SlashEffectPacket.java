package com.misanthropy.slashh.network;

import com.misanthropy.slashh.Slashh;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: draw one slash. Flags below are OR-ed into {@code flags}. */
public record SlashEffectPacket(double x, double y, double z, float rollDeg, float size, int rgb, int flags,
                                int attacker, float dx, float dy, float dz, String animation, int target)
        implements CustomPacketPayload {

    public static final int CRIT = 1;
    public static final int FINISHER = 2;
    public static final int SWEEP = 4;
    public static final int SPRAY = 8;
    public static final int OFFHAND = 16;

    public static final Type<SlashEffectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Slashh.MODID, "slash_effect"));

    public static final StreamCodec<FriendlyByteBuf, SlashEffectPacket> STREAM_CODEC =
            StreamCodec.ofMember(SlashEffectPacket::encode, SlashEffectPacket::decode);

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(rollDeg);
        buf.writeFloat(size);
        buf.writeInt(rgb);
        buf.writeByte(flags);
        buf.writeVarInt(attacker + 1);
        buf.writeFloat(dx);
        buf.writeFloat(dy);
        buf.writeFloat(dz);
        buf.writeUtf(animation, 128);
        buf.writeVarInt(target + 1);
    }

    public static SlashEffectPacket decode(FriendlyByteBuf buf) {
        return new SlashEffectPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(), buf.readInt(), buf.readByte(),
                buf.readVarInt() - 1,
                buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readUtf(128), buf.readVarInt() - 1);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
