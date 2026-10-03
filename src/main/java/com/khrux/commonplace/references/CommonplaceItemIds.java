package com.khrux.commonplace.references;

import com.khrux.commonplace.Commonplace;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class CommonplaceItemIds {
	public static final ResourceKey<Item> HANDBOOK = create("handbook");
	public static final ResourceKey<Item> ENDER_QUILL = create("ender_quill");

	private static ResourceKey<Item> create(final String name) {
		return ResourceKey.create(Registries.ITEM, Commonplace.id(name));
	}
}
