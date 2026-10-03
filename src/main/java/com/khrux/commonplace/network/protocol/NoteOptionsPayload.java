package com.khrux.commonplace.network.protocol;

import com.khrux.commonplace.Commonplace;
import com.khrux.commonplace.CommonplaceConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record NoteOptionsPayload(CommonplaceConfig.NoteOptions options) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, NoteOptionsPayload> STREAM_CODEC = StreamCodec.composite(
		CommonplaceConfig.NoteOptions.STREAM_CODEC, NoteOptionsPayload::options, NoteOptionsPayload::new
	);
	public static final CustomPacketPayload.Type<NoteOptionsPayload> TYPE = new CustomPacketPayload.Type<>(Commonplace.id("note_options"));

	@Override
	public CustomPacketPayload.Type<NoteOptionsPayload> type() {
		return TYPE;
	}
}
