package com.khrux.handbook.network.protocol;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.HandbookConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record NoteOptionsPayload(HandbookConfig.NoteOptions options) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, NoteOptionsPayload> STREAM_CODEC = StreamCodec.composite(
		HandbookConfig.NoteOptions.STREAM_CODEC, NoteOptionsPayload::options, NoteOptionsPayload::new
	);
	public static final CustomPacketPayload.Type<NoteOptionsPayload> TYPE = new CustomPacketPayload.Type<>(Handbook.id("note_options"));

	@Override
	public CustomPacketPayload.Type<NoteOptionsPayload> type() {
		return TYPE;
	}
}
