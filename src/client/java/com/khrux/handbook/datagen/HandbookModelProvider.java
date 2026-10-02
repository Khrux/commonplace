package com.khrux.handbook.datagen;

import com.khrux.handbook.world.item.HandbookItem;
import com.khrux.handbook.world.item.HandbookItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.color.item.Dye;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.resources.Identifier;

public class HandbookModelProvider extends FabricModelProvider {
	public HandbookModelProvider(final FabricPackOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(final BlockModelGenerators blockModelGenerators) {
	}

	@Override
	public void generateItemModels(final ItemModelGenerators itemModelGenerators) {
		Identifier model = itemModelGenerators.generateLayeredItem(
			ModelLocationUtils.getModelLocation(HandbookItems.HANDBOOK),
			TextureMapping.getItemTexture(HandbookItems.HANDBOOK),
			TextureMapping.getItemTexture(HandbookItems.HANDBOOK, "_overlay")
		);
		itemModelGenerators.itemModelOutput.accept(HandbookItems.HANDBOOK, ItemModelUtils.tintedModel(model, new Dye(HandbookItem.DEFAULT_COLOR)));
		itemModelGenerators.generateFlatItem(HandbookItems.ENDER_QUILL, ModelTemplates.FLAT_ITEM);
	}
}
