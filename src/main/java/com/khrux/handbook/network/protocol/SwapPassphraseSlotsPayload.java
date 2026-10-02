package com.khrux.handbook.network.protocol;

import com.khrux.handbook.Handbook;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SwapPassphraseSlotsPayload(int first, int second) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, SwapPassphraseSlotsPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, SwapPassphraseSlotsPayload::first, ByteBufCodecs.VAR_INT, SwapPassphraseSlotsPayload::second, SwapPassphraseSlotsPayload::new
	);
	public static final CustomPacketPayload.Type<SwapPassphraseSlotsPayload> TYPE = new CustomPacketPayload.Type<>(Handbook.id("swap_passphrase_slots"));

	@Override
	public CustomPacketPayload.Type<SwapPassphraseSlotsPayload> type() {
		return TYPE;
	}
}
