package com.khrux.commonplace.network.protocol;

import com.khrux.commonplace.Commonplace;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RemoveAtlasMarkerPayload(UUID id) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, RemoveAtlasMarkerPayload> STREAM_CODEC = StreamCodec.composite(
		UUIDUtil.STREAM_CODEC, RemoveAtlasMarkerPayload::id, RemoveAtlasMarkerPayload::new
	);
	public static final CustomPacketPayload.Type<RemoveAtlasMarkerPayload> TYPE = new CustomPacketPayload.Type<>(Commonplace.id("remove_atlas_marker"));

	@Override
	public CustomPacketPayload.Type<RemoveAtlasMarkerPayload> type() {
		return TYPE;
	}
}
