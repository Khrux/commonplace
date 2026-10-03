package com.khrux.commonplace.datagen;

import com.khrux.commonplace.world.item.CommonplaceItems;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;

public class CommonplaceLanguageProvider extends FabricLanguageProvider {
	public CommonplaceLanguageProvider(final FabricPackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	public void generateTranslations(final HolderLookup.Provider registries, final FabricLanguageProvider.TranslationBuilder translationBuilder) {
		translationBuilder.add(CommonplaceItems.HANDBOOK, "Handbook");
		translationBuilder.add("options.commonplace.title", "Commonplace Options");
		translationBuilder.add("options.commonplace.recipe_sheen", "Recipe Passphrase Colour");
		translationBuilder.add("options.commonplace.recipe_sheen.tooltip", "How recipes shared through a passphrase show its colour.");
		translationBuilder.add("options.commonplace.recipe_sheen.ink", "Inked Box Lines");
		translationBuilder.add("options.commonplace.recipe_sheen.square", "Square Outline");
		translationBuilder.add("options.commonplace.field_guide_sketch", "Field Guide Sketch");
		translationBuilder.add("options.commonplace.field_guide_sketch.tooltip", "Draw Field Guide images as sepia pencil sketches. Off shows Field Guide's own coloured images.");
		translationBuilder.add("options.commonplace.map_and_field_guide_sheen", "Map & Field Guide Colours");
		translationBuilder.add(
			"options.commonplace.map_and_field_guide_sheen.tooltip",
			"When the atlas and Field Guide show passphrase colours. Highlight Only keeps them in plain ink until you click a colour ribbon under the book."
		);
		translationBuilder.add("options.commonplace.map_and_field_guide_sheen.always", "Always");
		translationBuilder.add("options.commonplace.map_and_field_guide_sheen.highlighted", "Highlight Only");
		translationBuilder.add("options.commonplace.ender_page_motion", "Ender Page Motion");
		translationBuilder.add("options.commonplace.ender_page_motion.tooltip", "Shimmering ink and drifting stars on the Ender page. Off keeps them still.");
		translationBuilder.add("options.commonplace.compass_coordinates", "Compass Coordinates");
		translationBuilder.add("options.commonplace.compass_coordinates.tooltip", "Where your coordinates show while a compass is in the compass slot.");
		translationBuilder.add("options.commonplace.compass_coordinates.top_right", "Top Right");
		translationBuilder.add("options.commonplace.compass_coordinates.top_left", "Top Left");
		translationBuilder.add("options.commonplace.compass_coordinates.bottom_right", "Bottom Right");
		translationBuilder.add("options.commonplace.compass_coordinates.bottom_left", "Bottom Left");
		translationBuilder.add("options.commonplace.compass_coordinates.hidden", "Hidden");
		translationBuilder.add("options.commonplace.page_turn_sound", "Page Turn Sound");
		translationBuilder.add("options.commonplace.page_turn_sound.tooltip", "Play a page turn when scrolling through the tabs of the Handbook in your hand.");
		translationBuilder.add("options.commonplace.tab_scroll_key", "Tab Scroll Key");
		translationBuilder.add(
			"options.commonplace.tab_scroll_key.tooltip", "Hold this key and scroll with the Handbook in your hand to turn to the next or previous tab. Off scrolls the hotbar as usual."
		);
		translationBuilder.add("options.commonplace.tab_scroll_key.alt", "Alt");
		translationBuilder.add("options.commonplace.tab_scroll_key.control", "Ctrl");
		translationBuilder.add("options.commonplace.tab_scroll_key.shift", "Shift");
		translationBuilder.add("options.commonplace.tab_scroll_key.off", "Off");
		translationBuilder.add(CommonplaceItems.ENDER_QUILL, "Ender Quill");
		translationBuilder.add("commonplace.tab.ender", "Ender");
		translationBuilder.add("commonplace.ender.slot", "Passphrase %s");
		translationBuilder.add("commonplace.ender.passphrase", "Passphrase");
		translationBuilder.add("commonplace.ender.passphrase_hint", "Enter a passphrase...");
		translationBuilder.add("commonplace.ender.hidden_passphrase", "Hidden passphrase");
		translationBuilder.add("commonplace.ender.empty_slot", "Empty slot");
		translationBuilder.add("commonplace.ender.follow", "Follow");
		translationBuilder.add("commonplace.ender.leave", "Leave");
		translationBuilder.add("commonplace.ender.color", "Colour");
		translationBuilder.add("commonplace.ender.ink", "Ink");
		translationBuilder.add("commonplace.ender.ender_ink", "Ender ink");
		translationBuilder.add("commonplace.ender.ender_ink.description", "Everything you discover is shared under this passphrase.");
		translationBuilder.add("commonplace.ender.plain_ink", "Plain ink");
		translationBuilder.add("commonplace.ender.plain_ink.description", "Your discoveries are withheld from this passphrase. You still receive its updates.");
		translationBuilder.add("commonplace.ender.share_plain_ink", "Share my plain ink");
		translationBuilder.add("commonplace.ender.share_plain_ink.description", "Share everything you have that this passphrase doesn't, all at once.");
		translationBuilder.add("commonplace.ender.followers", "Followers (%s)");
		translationBuilder.add("commonplace.ender.sheen", "Passphrase colours");
		translationBuilder.add("commonplace.ender.no_followers", "Nobody yet. Enter a passphrase and press Follow to share your book with everyone who knows it.");
		translationBuilder.add("commonplace.tab.recipes", "Recipes");
		translationBuilder.add("commonplace.tab.field_guide", "Field Guide");
		translationBuilder.add("commonplace.tab.atlas", "Atlas");
		translationBuilder.add("commonplace.tab.notes", "Notes");
		translationBuilder.add("commonplace.recipes.search", "Search...");
		translationBuilder.add("commonplace.recipes.all", "All Recipes");
		translationBuilder.add("commonplace.recipes.equipment", "Equipment");
		translationBuilder.add("commonplace.recipes.building_blocks", "Building Blocks");
		translationBuilder.add("commonplace.recipes.misc", "Miscellaneous");
		translationBuilder.add("commonplace.recipes.redstone", "Redstone");
		translationBuilder.add("commonplace.recipes.smelting", "Smelting");
		translationBuilder.add("commonplace.recipes.other", "Other");
		translationBuilder.add("commonplace.recipes.station", "Handbook Recipes");
		translationBuilder.add("commonplace.notes.personal", "Personal Notebook");
		translationBuilder.add("commonplace.notes.pen", "Pen");
		translationBuilder.add("commonplace.notes.eraser", "Eraser");
		translationBuilder.add("commonplace.notes.text", "Text");
		translationBuilder.add("commonplace.notes.ink", "Ink");
		translationBuilder.add("commonplace.marker.point", "Point");
		translationBuilder.add("commonplace.marker.bed", "Bed");
		translationBuilder.add("commonplace.marker.tower", "Tower");
		translationBuilder.add("commonplace.marker.pickaxe", "Mine");
		translationBuilder.add("commonplace.marker.diamond", "Treasure");
		translationBuilder.add("commonplace.marker.brush", "Dig Site");
		translationBuilder.add("commonplace.marker.scroll", "Note");
		translationBuilder.add("commonplace.marker.sword", "Battle");
		translationBuilder.add("commonplace.marker.stuck_sword", "Grave");
		translationBuilder.add("commonplace.marker.skull", "Danger");
		translationBuilder.add("commonplace.marker.red_x_small", "Small X");
		translationBuilder.add("commonplace.marker.red_x_large", "X Marks the Spot");
		translationBuilder.add("commonplace.atlas.label", "Label...");
		translationBuilder.add("commonplace.compass.coordinates", "X %s  Y %s  Z %s");
		translationBuilder.add("key.category.commonplace.commonplace", "Commonplace");
		translationBuilder.add("key.commonplace.spyglass_zoom", "Spyglass Zoom (Handbook slot)");
		translationBuilder.add("key.commonplace.open_ender", "Open Ender");
		translationBuilder.add("key.commonplace.open_recipes", "Open Recipes");
		translationBuilder.add("key.commonplace.open_field_guide", "Open Field Guide");
		translationBuilder.add("key.commonplace.open_atlas", "Open Atlas");
		translationBuilder.add("key.commonplace.open_notes", "Open Notes");
		translationBuilder.add("commonplace.atlas.remove_marker", "Remove");
	}
}
