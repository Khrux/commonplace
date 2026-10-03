package com.khrux.handbook;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.slf4j.Logger;

public class HandbookConfig {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("handbook.json");
	public static final Codec<HandbookConfig> CODEC = RecordCodecBuilder.create(
		i -> i.group(
				HandbookConfig.NoteOptions.MAP_CODEC.forGetter(config -> config.noteOptions),
				HandbookConfig.NoteOptions.field(Codec.BOOL, "field_guide_sketch", true).forGetter(config -> config.fieldGuideSketch),
				HandbookConfig.NoteOptions.field(HandbookConfig.RecipeSheen.CODEC, "recipe_sheen", HandbookConfig.RecipeSheen.INK).forGetter(config -> config.recipeSheen),
				HandbookConfig.NoteOptions.field(HandbookConfig.ContentSheen.CODEC, "map_and_field_guide_sheen", HandbookConfig.ContentSheen.ALWAYS)
					.forGetter(config -> config.contentSheen),
				HandbookConfig.NoteOptions.field(Codec.BOOL, "ender_page_motion", true).forGetter(config -> config.enderPageMotion),
				HandbookConfig.NoteOptions.field(HandbookConfig.CoordinatesPosition.CODEC, "compass_coordinates", HandbookConfig.CoordinatesPosition.TOP_RIGHT)
					.forGetter(config -> config.coordinatesPosition),
				HandbookConfig.NoteOptions.field(Codec.BOOL, "page_turn_sound", true).forGetter(config -> config.pageTurnSound),
				HandbookConfig.NoteOptions.field(HandbookConfig.TabScrollKey.CODEC, "tab_scroll_key", HandbookConfig.TabScrollKey.ALT).forGetter(config -> config.tabScrollKey)
			)
			.apply(i, HandbookConfig::new)
	);
	private static HandbookConfig instance = new HandbookConfig(
		HandbookConfig.NoteOptions.DEFAULT,
		true,
		HandbookConfig.RecipeSheen.INK,
		HandbookConfig.ContentSheen.ALWAYS,
		true,
		HandbookConfig.CoordinatesPosition.TOP_RIGHT,
		true,
		HandbookConfig.TabScrollKey.ALT
	);
	public HandbookConfig.NoteOptions noteOptions;
	public boolean fieldGuideSketch;
	public HandbookConfig.RecipeSheen recipeSheen;
	public HandbookConfig.ContentSheen contentSheen;
	public boolean enderPageMotion;
	public HandbookConfig.CoordinatesPosition coordinatesPosition;
	public boolean pageTurnSound;
	public HandbookConfig.TabScrollKey tabScrollKey;

	private HandbookConfig(
		final HandbookConfig.NoteOptions noteOptions,
		final boolean fieldGuideSketch,
		final HandbookConfig.RecipeSheen recipeSheen,
		final HandbookConfig.ContentSheen contentSheen,
		final boolean enderPageMotion,
		final HandbookConfig.CoordinatesPosition coordinatesPosition,
		final boolean pageTurnSound,
		final HandbookConfig.TabScrollKey tabScrollKey
	) {
		this.noteOptions = noteOptions;
		this.fieldGuideSketch = fieldGuideSketch;
		this.recipeSheen = recipeSheen;
		this.contentSheen = contentSheen;
		this.enderPageMotion = enderPageMotion;
		this.coordinatesPosition = coordinatesPosition;
		this.pageTurnSound = pageTurnSound;
		this.tabScrollKey = tabScrollKey;
	}

	public static HandbookConfig get() {
		return instance;
	}

	public static HandbookConfig.NoteOptions getNoteOptions() {
		return instance.noteOptions;
	}

	public static void load() {
		if (!Files.isRegularFile(PATH)) {
			instance.save();
			return;
		}

		try {
			CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(PATH))).ifSuccess(config -> instance = config);
		} catch (IOException | RuntimeException e) {
			LOGGER.warn("Failed to read {}", PATH, e);
		}
	}

	public void save() {
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, new GsonBuilder().setPrettyPrinting().create().toJson(CODEC.encodeStart(JsonOps.INSTANCE, this).getOrThrow()));
		} catch (IOException e) {
			LOGGER.warn("Failed to save {}", PATH, e);
		}
	}

	public enum RecipeSheen implements StringRepresentable {
		INK("ink"),
		SQUARE("square");

		public static final Codec<HandbookConfig.RecipeSheen> CODEC = StringRepresentable.fromEnum(HandbookConfig.RecipeSheen::values);
		private final String name;

		RecipeSheen(final String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}

		public Component getDisplayName() {
			return Component.translatable("options.handbook.recipe_sheen." + this.name);
		}
	}

	public enum ContentSheen implements StringRepresentable {
		ALWAYS("always"),
		HIGHLIGHTED("highlighted");

		public static final Codec<HandbookConfig.ContentSheen> CODEC = StringRepresentable.fromEnum(HandbookConfig.ContentSheen::values);
		private final String name;

		ContentSheen(final String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}

		public Component getDisplayName() {
			return Component.translatable("options.handbook.map_and_field_guide_sheen." + this.name);
		}
	}

	public enum CoordinatesPosition implements StringRepresentable {
		TOP_RIGHT("top_right"),
		TOP_LEFT("top_left"),
		BOTTOM_RIGHT("bottom_right"),
		BOTTOM_LEFT("bottom_left"),
		HIDDEN("hidden");

		public static final Codec<HandbookConfig.CoordinatesPosition> CODEC = StringRepresentable.fromEnum(HandbookConfig.CoordinatesPosition::values);
		private final String name;

		CoordinatesPosition(final String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}

		public Component getDisplayName() {
			return Component.translatable("options.handbook.compass_coordinates." + this.name);
		}
	}

	public enum TabScrollKey implements StringRepresentable {
		ALT("alt"),
		CONTROL("control"),
		SHIFT("shift"),
		OFF("off");

		public static final Codec<HandbookConfig.TabScrollKey> CODEC = StringRepresentable.fromEnum(HandbookConfig.TabScrollKey::values);
		private final String name;

		TabScrollKey(final String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}

		public Component getDisplayName() {
			return Component.translatable("options.handbook.tab_scroll_key." + this.name);
		}
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
		public static final MapCodec<HandbookConfig.NoteOptions> MAP_CODEC = RecordCodecBuilder.mapCodec(
			i -> i.group(
					field(Codec.intRange(MIN_PAGES, MAX_PAGES), "passphrase_notebook_pages", DEFAULT.passphrasePages()).forGetter(HandbookConfig.NoteOptions::passphrasePages),
					field(HandbookConfig.NoteColors.CODEC, "note_colors", DEFAULT.colors()).forGetter(HandbookConfig.NoteOptions::colors),
					field(Codec.intRange(0, 3650), "notebook_fade_days", DEFAULT.fadeDays()).forGetter(HandbookConfig.NoteOptions::fadeDays)
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

		static <T> MapCodec<T> field(final Codec<T> codec, final String name, final T defaultValue) {
			return codec.optionalFieldOf(name).xmap(value -> value.orElse(defaultValue), Optional::of);
		}
	}
}
