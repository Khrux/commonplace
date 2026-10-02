package com.khrux.handbook.network.protocol;

import com.khrux.handbook.Handbook;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AtlasTilesPayload(
	boolean reset, List<String> palette, long[] positions, int[] tiles, long[] structurePositions, List<String> structureMarkers
) implements CustomPacketPayload {
	public static final int MAX_BATCH = 4096;
	private static final StreamCodec<ByteBuf, List<String>> PALETTE_CODEC = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(65536));
	public static final StreamCodec<FriendlyByteBuf, AtlasTilesPayload> STREAM_CODEC = StreamCodec.of(
		(buffer, payload) -> {
			buffer.writeBoolean(payload.reset);
			PALETTE_CODEC.encode(buffer, payload.palette);
			buffer.writeLongArray(payload.positions);
			buffer.writeVarIntArray(payload.tiles);
			buffer.writeLongArray(payload.structurePositions);
			PALETTE_CODEC.encode(buffer, payload.structureMarkers);
		},
		buffer -> new AtlasTilesPayload(
			buffer.readBoolean(), PALETTE_CODEC.decode(buffer), buffer.readLongArray(), buffer.readVarIntArray(), buffer.readLongArray(), PALETTE_CODEC.decode(buffer)
		)
	);
	public static final CustomPacketPayload.Type<AtlasTilesPayload> TYPE = new CustomPacketPayload.Type<>(Handbook.id("atlas_tiles"));

	@Override
	public CustomPacketPayload.Type<AtlasTilesPayload> type() {
		return TYPE;
	}
}
