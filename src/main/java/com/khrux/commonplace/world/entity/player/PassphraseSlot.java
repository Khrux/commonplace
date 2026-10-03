package com.khrux.commonplace.world.entity.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;

public record PassphraseSlot(String hash, int color, boolean enderInk) {
	public static final int SLOTS = 5;
	public static final int HASH_LENGTH = 64;
	public static final int MAX_PASSPHRASE_LENGTH = 32;
	private static final List<DyeColor> DEFAULT_COLORS = List.of(DyeColor.PURPLE, DyeColor.CYAN, DyeColor.LIME, DyeColor.ORANGE, DyeColor.RED);
	public static final Codec<PassphraseSlot> CODEC = RecordCodecBuilder.create(
		i -> i.group(
				Codec.STRING.fieldOf("hash").forGetter(PassphraseSlot::hash),
				Codec.intRange(0, 15).fieldOf("color").forGetter(PassphraseSlot::color),
				Codec.BOOL.fieldOf("ender_ink").forGetter(PassphraseSlot::enderInk)
			)
			.apply(i, PassphraseSlot::new)
	);
	public static final StreamCodec<ByteBuf, PassphraseSlot> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.stringUtf8(HASH_LENGTH),
		PassphraseSlot::hash,
		ByteBufCodecs.VAR_INT,
		PassphraseSlot::color,
		ByteBufCodecs.BOOL,
		PassphraseSlot::enderInk,
		PassphraseSlot::new
	);

	public static PassphraseSlot empty(final int index) {
		return new PassphraseSlot("", DEFAULT_COLORS.get(index).getId(), true);
	}

	public static List<PassphraseSlot> createSlots() {
		List<PassphraseSlot> slots = new ArrayList<>();
		for (int i = 0; i < SLOTS; i++) {
			slots.add(empty(i));
		}

		return List.copyOf(slots);
	}

	public static boolean isValidHash(final String hash) {
		if (hash.length() != HASH_LENGTH) {
			return false;
		}

		for (int i = 0; i < hash.length(); i++) {
			char c = hash.charAt(i);
			if ((c < '0' || c > '9') && (c < 'a' || c > 'f')) {
				return false;
			}
		}

		return true;
	}

	public boolean isEmpty() {
		return this.hash.isEmpty();
	}

	public PassphraseSlot withHash(final String newHash) {
		return new PassphraseSlot(newHash, this.color, true);
	}

	public PassphraseSlot withSettings(final int newColor, final boolean newEnderInk) {
		return new PassphraseSlot(this.hash, newColor, newEnderInk);
	}
}
