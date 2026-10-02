package com.khrux.handbook;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public class HandbookConfig {
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("handbook.json");
	private static HandbookConfig.NoteOptions noteOptions = HandbookConfig.NoteOptions.DEFAULT;

	public static HandbookConfig.NoteOptions getNoteOptions() {
		return noteOptions;
	}

	public static void load() {
		if (Files.isRegularFile(PATH)) {
			try {
				noteOptions = HandbookConfig.NoteOptions.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(PATH))).result().orElse(HandbookConfig.NoteOptions.DEFAULT);
				return;
			} catch (IOException | RuntimeException ignored) {
			}
		}

		noteOptions = HandbookConfig.NoteOptions.DEFAULT;
		HandbookConfig.NoteOptions.CODEC.encodeStart(JsonOps.INSTANCE, noteOptions).ifSuccess(json -> {
			try {
				Files.writeString(PATH, new GsonBuilder().setPrettyPrinting().create().toJson(json));
			} catch (IOException ignored) {
			}
		});
	}

	public enum NoteColors implements StringRepresentable {
		FULL("full"),
		BLACK("black"),
		TEXT("text");

		public static final Codec<HandbookConfig.NoteColors> CODEC = StringRepresentable.fromEnum(HandbookConfig.NoteColors::values);
		private final String name;

		NoteColors(final String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}

	public record NoteOptions(int passphrasePages, HandbookConfig.NoteColors colors, int fadeDays) {
		public static final int MIN_PAGES = 2;
		public static final int MAX_PAGES = 64;
		public static final HandbookConfig.NoteOptions DEFAULT = new HandbookConfig.NoteOptions(16, HandbookConfig.NoteColors.FULL, 7);
		public static final Codec<HandbookConfig.NoteOptions> CODEC = RecordCodecBuilder.create(
			i -> i.group(
					Codec.intRange(MIN_PAGES, MAX_PAGES).optionalFieldOf("passphrase_notebook_pages", DEFAULT.passphrasePages()).forGetter(HandbookConfig.NoteOptions::passphrasePages),
					HandbookConfig.NoteColors.CODEC.optionalFieldOf("note_colors", DEFAULT.colors()).forGetter(HandbookConfig.NoteOptions::colors),
					Codec.intRange(0, 3650).optionalFieldOf("notebook_fade_days", DEFAULT.fadeDays()).forGetter(HandbookConfig.NoteOptions::fadeDays)
				)
				.apply(i, HandbookConfig.NoteOptions::new)
		);
		public static final StreamCodec<ByteBuf, HandbookConfig.NoteOptions> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			HandbookConfig.NoteOptions::passphrasePages,
			ByteBufCodecs.idMapper(id -> HandbookConfig.NoteColors.values()[id], HandbookConfig.NoteColors::ordinal),
			HandbookConfig.NoteOptions::colors,
			ByteBufCodecs.VAR_INT,
			HandbookConfig.NoteOptions::fadeDays,
			HandbookConfig.NoteOptions::new
		);
	}
}
