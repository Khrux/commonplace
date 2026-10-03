package com.khrux.commonplace.network.protocol;

import com.khrux.commonplace.Commonplace;
import com.khrux.commonplace.world.entity.player.NotePage;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record NoteEditPayload(int book, int page, int[] pixels, Optional<String> text) implements CustomPacketPayload {
	public static final int PERSONAL = -1;
	public static final int MAX_PIXELS = 4096;
	public static final StreamCodec<FriendlyByteBuf, NoteEditPayload> STREAM_CODEC = StreamCodec.of(NoteEditPayload::write, NoteEditPayload::read);
	public static final CustomPacketPayload.Type<NoteEditPayload> TYPE = new CustomPacketPayload.Type<>(Commonplace.id("note_edit"));

	public static int pack(final int x, final int y, final int color) {
		return x | y << 7 | color << 14;
	}

	public static int getX(final int pixel) {
		return pixel & 127;
	}

	public static int getY(final int pixel) {
		return pixel >> 7 & 127;
	}

	public static int getColor(final int pixel) {
		return pixel >> 14 & 15;
	}

	private static void write(final FriendlyByteBuf buffer, final NoteEditPayload payload) {
		buffer.writeVarInt(payload.book);
		buffer.writeVarInt(payload.page);
		buffer.writeVarIntArray(payload.pixels);
		ByteBufCodecs.optional(ByteBufCodecs.stringUtf8(NotePage.MAX_TEXT_LENGTH)).encode(buffer, payload.text);
	}

	private static NoteEditPayload read(final FriendlyByteBuf buffer) {
		return new NoteEditPayload(
			buffer.readVarInt(), buffer.readVarInt(), buffer.readVarIntArray(MAX_PIXELS), ByteBufCodecs.optional(ByteBufCodecs.stringUtf8(NotePage.MAX_TEXT_LENGTH)).decode(buffer)
		);
	}

	@Override
	public CustomPacketPayload.Type<NoteEditPayload> type() {
		return TYPE;
	}
}
