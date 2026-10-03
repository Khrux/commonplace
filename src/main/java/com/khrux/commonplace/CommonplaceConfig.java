package com.khrux.commonplace;

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

public class CommonplaceConfig {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("commonplace.json");
	public static final Codec<CommonplaceConfig> CODEC = RecordCodecBuilder.create(
		i -> i.group(
				CommonplaceConfig.NoteOptions.MAP_CODEC.forGetter(config -> config.noteOptions),
				CommonplaceConfig.NoteOptions.field(Codec.BOOL, "field_guide_sketch", true).forGetter(config -> config.fieldGuideSketch),
				CommonplaceConfig.NoteOptions.field(CommonplaceConfig.RecipeSheen.CODEC, "recipe_sheen", CommonplaceConfig.RecipeSheen.INK).forGetter(config -> config.recipeSheen),
				CommonplaceConfig.NoteOptions.field(CommonplaceConfig.ContentSheen.CODEC, "map_and_field_guide_sheen", CommonplaceConfig.ContentSheen.ALWAYS)
					.forGetter(config -> config.contentSheen),
				CommonplaceConfig.NoteOptions.field(Codec.BOOL, "ender_page_motion", true).forGetter(config -> config.enderPageMotion),
				CommonplaceConfig.NoteOptions.field(CommonplaceConfig.CoordinatesPosition.CODEC, "compass_coordinates", CommonplaceConfig.CoordinatesPosition.TOP_RIGHT)
					.forGetter(config -> config.coordinatesPosition),
				CommonplaceConfig.NoteOptions.field(Codec.BOOL, "page_turn_sound", true).forGetter(config -> config.pageTurnSound),
				CommonplaceConfig.NoteOptions.field(CommonplaceConfig.TabScrollKey.CODEC, "tab_scroll_key", CommonplaceConfig.TabScrollKey.ALT).forGetter(config -> config.tabScrollKey)
			)
			.apply(i, CommonplaceConfig::new)
	);
	private static CommonplaceConfig instance = new CommonplaceConfig(
		CommonplaceConfig.NoteOptions.DEFAULT,
		true,
		CommonplaceConfig.RecipeSheen.INK,
		CommonplaceConfig.ContentSheen.ALWAYS,
		true,
		CommonplaceConfig.CoordinatesPosition.TOP_RIGHT,
		true,
		CommonplaceConfig.TabScrollKey.ALT
	);
	public CommonplaceConfig.NoteOptions noteOptions;
	public boolean fieldGuideSketch;
	public CommonplaceConfig.RecipeSheen recipeSheen;
	public CommonplaceConfig.ContentSheen contentSheen;
	public boolean enderPageMotion;
	public CommonplaceConfig.CoordinatesPosition coordinatesPosition;
	public boolean pageTurnSound;
	public CommonplaceConfig.TabScrollKey tabScrollKey;

	private CommonplaceConfig(
		final CommonplaceConfig.NoteOptions noteOptions,
		final boolean fieldGuideSketch,
		final CommonplaceConfig.RecipeSheen recipeSheen,
		final CommonplaceConfig.ContentSheen contentSheen,
		final boolean enderPageMotion,
		final CommonplaceConfig.CoordinatesPosition coordinatesPosition,
		final boolean pageTurnSound,
		final CommonplaceConfig.TabScrollKey tabScrollKey
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

	public static CommonplaceConfig get() {
		return instance;
	}

	public static CommonplaceConfig.NoteOptions getNoteOptions() {
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

		public static final Codec<CommonplaceConfig.RecipeSheen> CODEC = StringRepresentable.fromEnum(CommonplaceConfig.RecipeSheen::values);
		private final String name;

		RecipeSheen(final String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}

		public Component getDisplayName() {
			return Component.translatable("options.commonplace.recipe_sheen." + this.name);
		}
	}

	public enum ContentSheen implements StringRepresentable {
		ALWAYS("always"),
		HIGHLIGHTED("highlighted");

		public static final Codec<CommonplaceConfig.ContentSheen> CODEC = StringRepresentable.fromEnum(CommonplaceConfig.ContentSheen::values);
		private final String name;

		ContentSheen(final String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}

		public Component getDisplayName() {
			return Component.translatable("options.commonplace.map_and_field_guide_sheen." + this.name);
		}
	}

	public enum CoordinatesPosition implements StringRepresentable {
		TOP_RIGHT("top_right"),
		TOP_LEFT("top_left"),
		BOTTOM_RIGHT("bottom_right"),
		BOTTOM_LEFT("bottom_left"),
		HIDDEN("hidden");

		public static final Codec<CommonplaceConfig.CoordinatesPosition> CODEC = StringRepresentable.fromEnum(CommonplaceConfig.CoordinatesPosition::values);
		private final String name;

		CoordinatesPosition(final String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}

		public Component getDisplayName() {
			return Component.translatable("options.commonplace.compass_coordinates." + this.name);
		}
	}

	public enum TabScrollKey implements StringRepresentable {
		ALT("alt"),
		CONTROL("control"),
		SHIFT("shift"),
		OFF("off");

		public static final Codec<CommonplaceConfig.TabScrollKey> CODEC = StringRepresentable.fromEnum(CommonplaceConfig.TabScrollKey::values);
		private final String name;

		TabScrollKey(final String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}

		public Component getDisplayName() {
			return Component.translatable("options.commonplace.tab_scroll_key." + this.name);
		}
	}

	public enum NoteColors implements StringRepresentable {
		FULL("full"),
		BLACK("black"),
		TEXT("text");

		public static final Codec<CommonplaceConfig.NoteColors> CODEC = StringRepresentable.fromEnum(CommonplaceConfig.NoteColors::values);
		private final String name;

		NoteColors(final String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}

	public record NoteOptions(int passphrasePages, CommonplaceConfig.NoteColors colors, int fadeDays) {
		public static final int MIN_PAGES = 2;
		public static final int MAX_PAGES = 64;
		public static final CommonplaceConfig.NoteOptions DEFAULT = new CommonplaceConfig.NoteOptions(16, CommonplaceConfig.NoteColors.FULL, 7);
		public static final MapCodec<CommonplaceConfig.NoteOptions> MAP_CODEC = RecordCodecBuilder.mapCodec(
			i -> i.group(
					field(Codec.intRange(MIN_PAGES, MAX_PAGES), "passphrase_notebook_pages", DEFAULT.passphrasePages()).forGetter(CommonplaceConfig.NoteOptions::passphrasePages),
					field(CommonplaceConfig.NoteColors.CODEC, "note_colors", DEFAULT.colors()).forGetter(CommonplaceConfig.NoteOptions::colors),
					field(Codec.intRange(0, 3650), "notebook_fade_days", DEFAULT.fadeDays()).forGetter(CommonplaceConfig.NoteOptions::fadeDays)
				)
				.apply(i, CommonplaceConfig.NoteOptions::new)
		);
		public static final StreamCodec<ByteBuf, CommonplaceConfig.NoteOptions> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			CommonplaceConfig.NoteOptions::passphrasePages,
			ByteBufCodecs.idMapper(id -> CommonplaceConfig.NoteColors.values()[id], CommonplaceConfig.NoteColors::ordinal),
			CommonplaceConfig.NoteOptions::colors,
			ByteBufCodecs.VAR_INT,
			CommonplaceConfig.NoteOptions::fadeDays,
			CommonplaceConfig.NoteOptions::new
		);

		static <T> MapCodec<T> field(final Codec<T> codec, final String name, final T defaultValue) {
			return codec.optionalFieldOf(name).xmap(value -> value.orElse(defaultValue), Optional::of);
		}
	}
}
