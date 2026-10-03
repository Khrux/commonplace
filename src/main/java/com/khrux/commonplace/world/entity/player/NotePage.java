package com.khrux.commonplace.world.entity.player;

import com.khrux.commonplace.network.protocol.NoteEditPayload;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.ByteBuffer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record NotePage(String text, byte[] drawing) {
	public static final int PAGES = 64;
	public static final int MAX_TEXT_LENGTH = 512;
	public static final int CANVAS_WIDTH = 64;
	public static final int CANVAS_HEIGHT = 72;
	public static final int DRAWING_SIZE = CANVAS_WIDTH * CANVAS_HEIGHT / 2;
	public static final NotePage EMPTY = new NotePage("", new byte[0]);
	private static final Codec<byte[]> DRAWING_CODEC = Codec.BYTE_BUFFER.xmap(buffer -> {
		byte[] bytes = new byte[buffer.remaining()];
		buffer.get(bytes);
		return bytes;
	}, ByteBuffer::wrap);
	public static final Codec<NotePage> CODEC = RecordCodecBuilder.create(
		i -> i.group(
				Codec.string(0, MAX_TEXT_LENGTH).optionalFieldOf("text", "").forGetter(NotePage::text),
				DRAWING_CODEC.optionalFieldOf("drawing", new byte[0]).forGetter(NotePage::drawing)
			)
			.apply(i, NotePage::new)
	);
	public static final StreamCodec<FriendlyByteBuf, NotePage> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.stringUtf8(MAX_TEXT_LENGTH), NotePage::text, ByteBufCodecs.byteArray(DRAWING_SIZE), NotePage::drawing, NotePage::new
	);

	public boolean isValid() {
		return this.text.length() <= MAX_TEXT_LENGTH && (this.drawing.length == 0 || this.drawing.length == DRAWING_SIZE);
	}

	public int getPixel(final int x, final int y) {
		if (this.drawing.length == 0) {
			return 0;
		}

		int index = y * CANVAS_WIDTH + x;
		int packed = this.drawing[index / 2];
		return index % 2 == 0 ? packed & 15 : packed >> 4 & 15;
	}

	public NotePage withPixels(final int[] pixels) {
		if (pixels.length == 0) {
			return this;
		}

		byte[] copy = this.drawing.length == 0 ? new byte[DRAWING_SIZE] : this.drawing.clone();
		for (int pixel : pixels) {
			int x = NoteEditPayload.getX(pixel);
			int y = NoteEditPayload.getY(pixel);
			if (x >= CANVAS_WIDTH || y >= CANVAS_HEIGHT) {
				continue;
			}

			int index = y * CANVAS_WIDTH + x;
			int packed = copy[index / 2];
			int color = NoteEditPayload.getColor(pixel);
			copy[index / 2] = (byte)(index % 2 == 0 ? packed & 240 | color : packed & 15 | color << 4);
		}

		return new NotePage(this.text, copy);
	}

	public NotePage withText(final String text) {
		return new NotePage(text, this.drawing);
	}
}
