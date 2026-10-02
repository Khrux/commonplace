package com.khrux.handbook.world.item;

import com.khrux.handbook.references.HandbookItemIds;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class HandbookItems {
	public static final Item HANDBOOK = registerItem(HandbookItemIds.HANDBOOK, HandbookItem::new, new Item.Properties().stacksTo(1));

	private static Item registerItem(final ResourceKey<Item> id, final Function<Item.Properties, Item> itemFactory, final Item.Properties properties) {
		return Registry.register(BuiltInRegistries.ITEM, id, itemFactory.apply(properties.setId(id)));
	}

	public static void bootstrap() {
	}
}
