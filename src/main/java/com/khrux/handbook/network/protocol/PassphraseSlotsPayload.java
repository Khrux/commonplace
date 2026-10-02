package com.khrux.handbook.network.protocol;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.world.entity.player.PassphraseSlot;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PassphraseSlotsPayload(List<PassphraseSlotsPayload.SlotView> slots) implements CustomPacketPayload {
	public static final int MAX_FOLLOWERS = 256;
	public static final StreamCodec<FriendlyByteBuf, PassphraseSlotsPayload> STREAM_CODEC = StreamCodec.composite(
		PassphraseSlotsPayload.SlotView.STREAM_CODEC.apply(ByteBufCodecs.list(PassphraseSlot.SLOTS)), PassphraseSlotsPayload::slots, PassphraseSlotsPayload::new
	);
	public static final CustomPacketPayload.Type<PassphraseSlotsPayload> TYPE = new CustomPacketPayload.Type<>(Handbook.id("passphrase_slots"));

	@Override
	public CustomPacketPayload.Type<PassphraseSlotsPayload> type() {
		return TYPE;
	}

	public record SlotView(PassphraseSlot slot, List<PassphraseSlotsPayload.FollowerView> followers) {
		public static final StreamCodec<ByteBuf, PassphraseSlotsPayload.SlotView> STREAM_CODEC = StreamCodec.composite(
			PassphraseSlot.STREAM_CODEC,
			PassphraseSlotsPayload.SlotView::slot,
			PassphraseSlotsPayload.FollowerView.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_FOLLOWERS)),
			PassphraseSlotsPayload.SlotView::followers,
			PassphraseSlotsPayload.SlotView::new
		);
	}

	public record FollowerView(String name, boolean enderInk, boolean online) {
		public static final StreamCodec<ByteBuf, PassphraseSlotsPayload.FollowerView> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(16),
			PassphraseSlotsPayload.FollowerView::name,
			ByteBufCodecs.BOOL,
			PassphraseSlotsPayload.FollowerView::enderInk,
			ByteBufCodecs.BOOL,
			PassphraseSlotsPayload.FollowerView::online,
			PassphraseSlotsPayload.FollowerView::new
		);
	}
}
