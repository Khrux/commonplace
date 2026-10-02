package com.khrux.handbook.references;

import com.khrux.handbook.Handbook;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class HandbookItemIds {
	public static final ResourceKey<Item> HANDBOOK = create("handbook");

	private static ResourceKey<Item> create(final String name) {
		return ResourceKey.create(Registries.ITEM, Handbook.id(name));
	}
}
