package com.khrux.handbook.network.protocol;

import com.khrux.handbook.Handbook;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PassphraseSettingsPayload(int slot, int color, boolean enderInk) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, PassphraseSettingsPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		PassphraseSettingsPayload::slot,
		ByteBufCodecs.VAR_INT,
		PassphraseSettingsPayload::color,
		ByteBufCodecs.BOOL,
		PassphraseSettingsPayload::enderInk,
		PassphraseSettingsPayload::new
	);
	public static final CustomPacketPayload.Type<PassphraseSettingsPayload> TYPE = new CustomPacketPayload.Type<>(Handbook.id("passphrase_settings"));

	@Override
	public CustomPacketPayload.Type<PassphraseSettingsPayload> type() {
		return TYPE;
	}
}
