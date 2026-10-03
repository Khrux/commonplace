package com.khrux.commonplace.network.protocol;

import com.khrux.commonplace.Commonplace;
import com.khrux.commonplace.world.level.atlas.AtlasMarker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PlaceAtlasMarkerPayload(AtlasMarker.Type markerType, int x, int z, String label) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, PlaceAtlasMarkerPayload> STREAM_CODEC = StreamCodec.composite(
		AtlasMarker.Type.STREAM_CODEC,
		PlaceAtlasMarkerPayload::markerType,
		ByteBufCodecs.VAR_INT,
		PlaceAtlasMarkerPayload::x,
		ByteBufCodecs.VAR_INT,
		PlaceAtlasMarkerPayload::z,
		ByteBufCodecs.stringUtf8(AtlasMarker.MAX_LABEL_LENGTH),
		PlaceAtlasMarkerPayload::label,
		PlaceAtlasMarkerPayload::new
	);
	public static final CustomPacketPayload.Type<PlaceAtlasMarkerPayload> TYPE = new CustomPacketPayload.Type<>(Commonplace.id("place_atlas_marker"));

	@Override
	public CustomPacketPayload.Type<PlaceAtlasMarkerPayload> type() {
		return TYPE;
	}
}
