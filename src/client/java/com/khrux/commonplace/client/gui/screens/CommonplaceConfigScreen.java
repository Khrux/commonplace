package com.khrux.commonplace.client.gui.screens;

import com.khrux.commonplace.CommonplaceConfig;
import com.khrux.commonplace.client.ClientHandbook;
import com.khrux.commonplace.client.compat.FieldGuideSketch;
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

public class CommonplaceConfigScreen extends OptionsSubScreen {
	private static final Component TITLE = Component.translatable("options.commonplace.title");

	public CommonplaceConfigScreen(final Screen lastScreen) {
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
		CommonplaceConfig config = CommonplaceConfig.get();
		this.list.addBig(
			choice(
				"options.commonplace.recipe_sheen",
				CommonplaceConfig.RecipeSheen.values(),
				CommonplaceConfig.RecipeSheen.CODEC,
				CommonplaceConfig.RecipeSheen::getDisplayName,
				config.recipeSheen,
				value -> config.recipeSheen = value
			)
		);
		this.list.addBig(
			choice(
				"options.commonplace.map_and_field_guide_sheen",
				CommonplaceConfig.ContentSheen.values(),
				CommonplaceConfig.ContentSheen.CODEC,
				CommonplaceConfig.ContentSheen::getDisplayName,
				config.contentSheen,
				value -> {
					config.contentSheen = value;
					refreshSheen();
				}
			)
		);
		this.list.addSmall(
			toggle("options.commonplace.field_guide_sketch", config.fieldGuideSketch, value -> {
				config.fieldGuideSketch = value;
				refreshSheen();
			}),
			toggle("options.commonplace.ender_page_motion", config.enderPageMotion, value -> config.enderPageMotion = value)
		);
		this.list.addBig(
			choice(
				"options.commonplace.compass_coordinates",
				CommonplaceConfig.CoordinatesPosition.values(),
				CommonplaceConfig.CoordinatesPosition.CODEC,
				CommonplaceConfig.CoordinatesPosition::getDisplayName,
				config.coordinatesPosition,
				value -> config.coordinatesPosition = value
			)
		);
		this.list.addSmall(
			toggle("options.commonplace.page_turn_sound", config.pageTurnSound, value -> config.pageTurnSound = value),
			choice(
				"options.commonplace.tab_scroll_key",
				CommonplaceConfig.TabScrollKey.values(),
				CommonplaceConfig.TabScrollKey.CODEC,
				CommonplaceConfig.TabScrollKey::getDisplayName,
				config.tabScrollKey,
				value -> config.tabScrollKey = value
			)
		);
	}

	@Override
	public void removed() {
		super.removed();
		CommonplaceConfig.get().save();
	}
}
