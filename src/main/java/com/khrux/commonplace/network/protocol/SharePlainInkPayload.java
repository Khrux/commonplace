package com.khrux.commonplace.network.protocol;

import com.khrux.commonplace.Commonplace;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SharePlainInkPayload(int slot) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, SharePlainInkPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, SharePlainInkPayload::slot, SharePlainInkPayload::new
	);
	public static final CustomPacketPayload.Type<SharePlainInkPayload> TYPE = new CustomPacketPayload.Type<>(Commonplace.id("share_plain_ink"));

	@Override
	public CustomPacketPayload.Type<SharePlainInkPayload> type() {
		return TYPE;
	}
}
