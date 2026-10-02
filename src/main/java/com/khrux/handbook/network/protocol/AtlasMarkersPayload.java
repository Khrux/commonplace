package com.khrux.handbook.network.protocol;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.world.level.atlas.AtlasMarker;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AtlasMarkersPayload(List<AtlasMarker> markers) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, AtlasMarkersPayload> STREAM_CODEC = StreamCodec.composite(
		AtlasMarker.STREAM_CODEC.apply(ByteBufCodecs.list()), AtlasMarkersPayload::markers, AtlasMarkersPayload::new
	);
	public static final CustomPacketPayload.Type<AtlasMarkersPayload> TYPE = new CustomPacketPayload.Type<>(Handbook.id("atlas_markers"));

	@Override
	public CustomPacketPayload.Type<AtlasMarkersPayload> type() {
		return TYPE;
	}
}
