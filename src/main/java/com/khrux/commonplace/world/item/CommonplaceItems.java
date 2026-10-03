package com.khrux.commonplace.world.item;

import com.khrux.commonplace.references.CommonplaceItemIds;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class CommonplaceItems {
	public static final Item HANDBOOK = registerItem(CommonplaceItemIds.HANDBOOK, HandbookItem::new, new Item.Properties().stacksTo(1));
	public static final Item ENDER_QUILL = registerItem(CommonplaceItemIds.ENDER_QUILL, Item::new, new Item.Properties().stacksTo(16));

	private static Item registerItem(final ResourceKey<Item> id, final Function<Item.Properties, Item> itemFactory, final Item.Properties properties) {
		return Registry.register(BuiltInRegistries.ITEM, id, itemFactory.apply(properties.setId(id)));
	}

	public static void bootstrap() {
	}
}
