package com.khrux.handbook.datagen;

import com.khrux.handbook.world.item.HandbookItems;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;

public class HandbookLanguageProvider extends FabricLanguageProvider {
	public HandbookLanguageProvider(final FabricPackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	public void generateTranslations(final HolderLookup.Provider registries, final FabricLanguageProvider.TranslationBuilder translationBuilder) {
		translationBuilder.add(HandbookItems.HANDBOOK, "Handbook");
		translationBuilder.add("options.handbook.title", "Handbook Options");
		translationBuilder.add("options.handbook.recipe_sheen", "Recipe Passphrase Colour");
		translationBuilder.add("options.handbook.recipe_sheen.tooltip", "How recipes shared through a passphrase show its colour.");
		translationBuilder.add("options.handbook.recipe_sheen.ink", "Inked Box Lines");
		translationBuilder.add("options.handbook.recipe_sheen.square", "Square Outline");
		translationBuilder.add("options.handbook.field_guide_sketch", "Field Guide Sketch");
		translationBuilder.add("options.handbook.field_guide_sketch.tooltip", "Draw Field Guide images as sepia pencil sketches. Off shows Field Guide's own coloured images.");
		translationBuilder.add("options.handbook.map_and_field_guide_sheen", "Map & Field Guide Colours");
		translationBuilder.add(
			"options.handbook.map_and_field_guide_sheen.tooltip",
			"When the atlas and Field Guide show passphrase colours. Highlight Only keeps them in plain ink until you click a colour ribbon under the book."
		);
		translationBuilder.add("options.handbook.map_and_field_guide_sheen.always", "Always");
		translationBuilder.add("options.handbook.map_and_field_guide_sheen.highlighted", "Highlight Only");
		translationBuilder.add("options.handbook.ender_page_motion", "Ender Page Motion");
		translationBuilder.add("options.handbook.ender_page_motion.tooltip", "Shimmering ink and drifting stars on the Ender page. Off keeps them still.");
		translationBuilder.add("options.handbook.compass_coordinates", "Compass Coordinates");
		translationBuilder.add("options.handbook.compass_coordinates.tooltip", "Where your coordinates show while a compass is in the compass slot.");
		translationBuilder.add("options.handbook.compass_coordinates.top_right", "Top Right");
		translationBuilder.add("options.handbook.compass_coordinates.top_left", "Top Left");
		translationBuilder.add("options.handbook.compass_coordinates.bottom_right", "Bottom Right");
		translationBuilder.add("options.handbook.compass_coordinates.bottom_left", "Bottom Left");
		translationBuilder.add("options.handbook.compass_coordinates.hidden", "Hidden");
		translationBuilder.add("options.handbook.page_turn_sound", "Page Turn Sound");
		translationBuilder.add("options.handbook.page_turn_sound.tooltip", "Play a page turn when scrolling through the tabs of the Handbook in your hand.");
		translationBuilder.add("options.handbook.tab_scroll_key", "Tab Scroll Key");
		translationBuilder.add(
			"options.handbook.tab_scroll_key.tooltip", "Hold this key and scroll with the Handbook in your hand to turn to the next or previous tab. Off scrolls the hotbar as usual."
		);
		translationBuilder.add("options.handbook.tab_scroll_key.alt", "Alt");
		translationBuilder.add("options.handbook.tab_scroll_key.control", "Ctrl");
		translationBuilder.add("options.handbook.tab_scroll_key.shift", "Shift");
		translationBuilder.add("options.handbook.tab_scroll_key.off", "Off");
		translationBuilder.add(HandbookItems.ENDER_QUILL, "Ender Quill");
		translationBuilder.add("handbook.tab.ender", "Ender");
		translationBuilder.add("handbook.ender.slot", "Passphrase %s");
		translationBuilder.add("handbook.ender.passphrase", "Passphrase");
		translationBuilder.add("handbook.ender.passphrase_hint", "Enter a passphrase...");
		translationBuilder.add("handbook.ender.hidden_passphrase", "Hidden passphrase");
		translationBuilder.add("handbook.ender.empty_slot", "Empty slot");
		translationBuilder.add("handbook.ender.follow", "Follow");
		translationBuilder.add("handbook.ender.leave", "Leave");
		translationBuilder.add("handbook.ender.color", "Colour");
		translationBuilder.add("handbook.ender.ink", "Ink");
		translationBuilder.add("handbook.ender.ender_ink", "Ender ink");
		translationBuilder.add("handbook.ender.ender_ink.description", "Everything you discover is shared under this passphrase.");
		translationBuilder.add("handbook.ender.plain_ink", "Plain ink");
		translationBuilder.add("handbook.ender.plain_ink.description", "Your discoveries are withheld from this passphrase. You still receive its updates.");
		translationBuilder.add("handbook.ender.share_plain_ink", "Share my plain ink");
		translationBuilder.add("handbook.ender.share_plain_ink.description", "Share everything you have that this passphrase doesn't, all at once.");
		translationBuilder.add("handbook.ender.followers", "Followers (%s)");
		translationBuilder.add("handbook.ender.sheen", "Passphrase colours");
		translationBuilder.add("handbook.ender.no_followers", "Nobody yet. Enter a passphrase and press Follow to share your book with everyone who knows it.");
		translationBuilder.add("handbook.tab.recipes", "Recipes");
		translationBuilder.add("handbook.tab.field_guide", "Field Guide");
		translationBuilder.add("handbook.tab.atlas", "Atlas");
		translationBuilder.add("handbook.tab.notes", "Notes");
		translationBuilder.add("handbook.recipes.search", "Search...");
		translationBuilder.add("handbook.recipes.all", "All Recipes");
		translationBuilder.add("handbook.recipes.equipment", "Equipment");
		translationBuilder.add("handbook.recipes.building_blocks", "Building Blocks");
		translationBuilder.add("handbook.recipes.misc", "Miscellaneous");
		translationBuilder.add("handbook.recipes.redstone", "Redstone");
		translationBuilder.add("handbook.recipes.smelting", "Smelting");
		translationBuilder.add("handbook.recipes.other", "Other");
		translationBuilder.add("handbook.recipes.station", "Handbook Recipes");
		translationBuilder.add("handbook.notes.personal", "Personal Notebook");
		translationBuilder.add("handbook.notes.pen", "Pen");
		translationBuilder.add("handbook.notes.eraser", "Eraser");
		translationBuilder.add("handbook.notes.text", "Text");
		translationBuilder.add("handbook.notes.ink", "Ink");
		translationBuilder.add("handbook.marker.point", "Point");
		translationBuilder.add("handbook.marker.bed", "Bed");
		translationBuilder.add("handbook.marker.tower", "Tower");
		translationBuilder.add("handbook.marker.pickaxe", "Mine");
		translationBuilder.add("handbook.marker.diamond", "Treasure");
		translationBuilder.add("handbook.marker.brush", "Dig Site");
		translationBuilder.add("handbook.marker.scroll", "Note");
		translationBuilder.add("handbook.marker.sword", "Battle");
		translationBuilder.add("handbook.marker.stuck_sword", "Grave");
		translationBuilder.add("handbook.marker.skull", "Danger");
		translationBuilder.add("handbook.marker.red_x_small", "Small X");
		translationBuilder.add("handbook.marker.red_x_large", "X Marks the Spot");
		translationBuilder.add("handbook.atlas.label", "Label...");
		translationBuilder.add("handbook.compass.coordinates", "X %s  Y %s  Z %s");
		translationBuilder.add("key.category.handbook.handbook", "Handbook");
		translationBuilder.add("key.handbook.spyglass_zoom", "Spyglass Zoom (Handbook slot)");
		translationBuilder.add("key.handbook.open_ender", "Open Ender");
		translationBuilder.add("key.handbook.open_recipes", "Open Recipes");
		translationBuilder.add("key.handbook.open_field_guide", "Open Field Guide");
		translationBuilder.add("key.handbook.open_atlas", "Open Atlas");
		translationBuilder.add("key.handbook.open_notes", "Open Notes");
		translationBuilder.add("handbook.atlas.remove_marker", "Remove");
	}
}
