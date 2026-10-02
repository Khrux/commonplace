package com.khrux.handbook.network.protocol;

import com.khrux.handbook.Handbook;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SheenPayload(boolean reset, int layer, List<Integer> recipes, List<String> entries, String dimension, long[] chunks) implements CustomPacketPayload {
	public static final int LEFT_LAYER = 5;
	public static final int MAX_BATCH = 8192;
	public static final StreamCodec<FriendlyByteBuf, SheenPayload> STREAM_CODEC = StreamCodec.of(SheenPayload::write, SheenPayload::read);
	public static final CustomPacketPayload.Type<SheenPayload> TYPE = new CustomPacketPayload.Type<>(Handbook.id("sheen"));

	private static void write(final FriendlyByteBuf buffer, final SheenPayload payload) {
		buffer.writeBoolean(payload.reset);
		buffer.writeVarInt(payload.layer);
		ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(MAX_BATCH)).encode(buffer, payload.recipes);
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(MAX_BATCH)).encode(buffer, payload.entries);
		buffer.writeUtf(payload.dimension);
		buffer.writeLongArray(payload.chunks);
	}

	private static SheenPayload read(final FriendlyByteBuf buffer) {
		return new SheenPayload(
			buffer.readBoolean(),
			buffer.readVarInt(),
			ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(MAX_BATCH)).decode(buffer),
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(MAX_BATCH)).decode(buffer),
			buffer.readUtf(),
			buffer.readLongArray()
		);
	}

	@Override
	public CustomPacketPayload.Type<SheenPayload> type() {
		return TYPE;
	}
}
