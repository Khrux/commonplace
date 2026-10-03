package com.khrux.commonplace.network.protocol;

import com.khrux.commonplace.Commonplace;
import com.khrux.commonplace.world.entity.player.NotePage;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record NotebookPayload(int book, List<NotePage> pages) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, NotebookPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, NotebookPayload::book, NotePage.STREAM_CODEC.apply(ByteBufCodecs.list(NotePage.PAGES)), NotebookPayload::pages, NotebookPayload::new
	);
	public static final CustomPacketPayload.Type<NotebookPayload> TYPE = new CustomPacketPayload.Type<>(Commonplace.id("notebook"));

	@Override
	public CustomPacketPayload.Type<NotebookPayload> type() {
		return TYPE;
	}
}
