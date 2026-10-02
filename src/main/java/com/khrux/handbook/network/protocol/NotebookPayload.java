package com.khrux.handbook.network.protocol;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.world.entity.player.NotePage;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record NotebookPayload(List<NotePage> pages) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, NotebookPayload> STREAM_CODEC = StreamCodec.composite(
		NotePage.STREAM_CODEC.apply(ByteBufCodecs.list(NotePage.PAGES)), NotebookPayload::pages, NotebookPayload::new
	);
	public static final CustomPacketPayload.Type<NotebookPayload> TYPE = new CustomPacketPayload.Type<>(Handbook.id("notebook"));

	@Override
	public CustomPacketPayload.Type<NotebookPayload> type() {
		return TYPE;
	}
}
