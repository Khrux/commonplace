package com.khrux.handbook.network.protocol;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.world.entity.player.PassphraseSlot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FollowPassphrasePayload(int slot, String hash) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, FollowPassphrasePayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		FollowPassphrasePayload::slot,
		ByteBufCodecs.stringUtf8(PassphraseSlot.HASH_LENGTH),
		FollowPassphrasePayload::hash,
		FollowPassphrasePayload::new
	);
	public static final CustomPacketPayload.Type<FollowPassphrasePayload> TYPE = new CustomPacketPayload.Type<>(Handbook.id("follow_passphrase"));

	@Override
	public CustomPacketPayload.Type<FollowPassphrasePayload> type() {
		return TYPE;
	}
}
