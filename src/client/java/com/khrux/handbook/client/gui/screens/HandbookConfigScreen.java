package com.khrux.handbook.client.gui.screens;

import com.khrux.handbook.HandbookConfig;
import com.khrux.handbook.client.ClientHandbook;
import com.khrux.handbook.client.compat.FieldGuideSketch;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public class HandbookConfigScreen extends OptionsSubScreen {
	private static final Component TITLE = Component.translatable("options.handbook.title");

	public HandbookConfigScreen(final Screen lastScreen) {
		super(lastScreen, Minecraft.getInstance().options, TITLE);
	}

	private static <T> OptionInstance<T> choice(
		final String captionId, final T[] values, final Codec<T> codec, final Function<T, Component> displayName, final T initialValue, final Consumer<T> onValueUpdate
	) {
		return new OptionInstance<>(
			captionId,
			OptionInstance.cachedConstantTooltip(Component.translatable(captionId + ".tooltip")),
			(caption, value) -> displayName.apply(value),
			new OptionInstance.Enum<>(List.of(values), codec),
			initialValue,
			onValueUpdate::accept
		);
	}

	private static OptionInstance<Boolean> toggle(final String captionId, final boolean initialValue, final Consumer<Boolean> onValueUpdate) {
		return OptionInstance.createBoolean(captionId, OptionInstance.cachedConstantTooltip(Component.translatable(captionId + ".tooltip")), initialValue, onValueUpdate::accept);
	}

	private static void refreshSheen() {
		if (Minecraft.getInstance().level != null) {
			ClientHandbook.get().sheen().changed();
		}

		if (FabricLoader.getInstance().isModLoaded("fieldguide")) {
			FieldGuideSketch.refresh();
		}
	}

	@Override
	protected void addOptions() {
		HandbookConfig config = HandbookConfig.get();
		this.list.addBig(
			choice(
				"options.handbook.recipe_sheen",
				HandbookConfig.RecipeSheen.values(),
				HandbookConfig.RecipeSheen.CODEC,
				HandbookConfig.RecipeSheen::getDisplayName,
				config.recipeSheen,
				value -> config.recipeSheen = value
			)
		);
		this.list.addBig(
			choice(
				"options.handbook.map_and_field_guide_sheen",
				HandbookConfig.ContentSheen.values(),
				HandbookConfig.ContentSheen.CODEC,
				HandbookConfig.ContentSheen::getDisplayName,
				config.contentSheen,
				value -> {
					config.contentSheen = value;
					refreshSheen();
				}
			)
		);
		this.list.addSmall(
			toggle("options.handbook.field_guide_sketch", config.fieldGuideSketch, value -> {
				config.fieldGuideSketch = value;
				refreshSheen();
			}),
			toggle("options.handbook.ender_page_motion", config.enderPageMotion, value -> config.enderPageMotion = value)
		);
		this.list.addBig(
			choice(
				"options.handbook.compass_coordinates",
				HandbookConfig.CoordinatesPosition.values(),
				HandbookConfig.CoordinatesPosition.CODEC,
				HandbookConfig.CoordinatesPosition::getDisplayName,
				config.coordinatesPosition,
				value -> config.coordinatesPosition = value
			)
		);
		this.list.addSmall(
			toggle("options.handbook.page_turn_sound", config.pageTurnSound, value -> config.pageTurnSound = value),
			choice(
				"options.handbook.tab_scroll_key",
				HandbookConfig.TabScrollKey.values(),
				HandbookConfig.TabScrollKey.CODEC,
				HandbookConfig.TabScrollKey::getDisplayName,
				config.tabScrollKey,
				value -> config.tabScrollKey = value
			)
		);
	}

	@Override
	public void removed() {
		super.removed();
		HandbookConfig.get().save();
	}
}
