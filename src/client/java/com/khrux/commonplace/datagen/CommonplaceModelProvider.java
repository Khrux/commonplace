package com.khrux.commonplace.datagen;

import com.khrux.commonplace.world.item.HandbookItem;
import com.khrux.commonplace.world.item.CommonplaceItems;
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

public class CommonplaceModelProvider extends FabricModelProvider {
	public CommonplaceModelProvider(final FabricPackOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(final BlockModelGenerators blockModelGenerators) {
	}

	@Override
	public void generateItemModels(final ItemModelGenerators itemModelGenerators) {
		Identifier model = itemModelGenerators.generateLayeredItem(
			ModelLocationUtils.getModelLocation(CommonplaceItems.HANDBOOK),
			TextureMapping.getItemTexture(CommonplaceItems.HANDBOOK),
			TextureMapping.getItemTexture(CommonplaceItems.HANDBOOK, "_overlay")
		);
		itemModelGenerators.itemModelOutput.accept(CommonplaceItems.HANDBOOK, ItemModelUtils.tintedModel(model, new Dye(HandbookItem.DEFAULT_COLOR)));
		itemModelGenerators.generateFlatItem(CommonplaceItems.ENDER_QUILL, ModelTemplates.FLAT_ITEM);
	}
}
