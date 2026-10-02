package com.khrux.handbook.network.protocol;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.world.entity.player.NotePage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record NotePagePayload(int page, NotePage notePage) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, NotePagePayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, NotePagePayload::page, NotePage.STREAM_CODEC, NotePagePayload::notePage, NotePagePayload::new
	);
	public static final CustomPacketPayload.Type<NotePagePayload> TYPE = new CustomPacketPayload.Type<>(Handbook.id("note_page"));

	@Override
	public CustomPacketPayload.Type<NotePagePayload> type() {
		return TYPE;
	}
}
