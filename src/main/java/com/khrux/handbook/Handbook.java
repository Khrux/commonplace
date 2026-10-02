package com.khrux.handbook;

import com.khrux.handbook.compat.FieldGuideCompat;
import com.khrux.handbook.network.NotebookSync;
import com.khrux.handbook.world.entity.player.HandbookAttachmentTypes;
import com.khrux.handbook.world.inventory.ToolSlot;
import com.khrux.handbook.world.item.HandbookItem;
import com.khrux.handbook.world.item.HandbookItems;
import com.khrux.handbook.world.level.atlas.AtlasTracker;
import com.khrux.handbook.world.level.syndicate.Syndication;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

public class Handbook implements ModInitializer {
	public static final String MOD_ID = "handbook";

	@Override
	public void onInitialize() {
		HandbookItems.bootstrap();
		HandbookAttachmentTypes.bootstrap();
		NotebookSync.bootstrap();
		AtlasTracker.bootstrap();
		Syndication.bootstrap();
		ServerTickEvents.END_SERVER_TICK.register(HandbookItem::tick);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> ToolSlot.dropOnDeath(entity));
		ServerEntityEvents.ENTITY_LOAD.register(HandbookItem::entityLoad);
		UseEntityCallback.EVENT.register(HandbookItem::interact);
		UseBlockCallback.EVENT.register(HandbookItem::useOnBlock);
		if (FabricLoader.getInstance().isModLoaded("fieldguide")) {
			FieldGuideCompat.bootstrap();
		}
	}

	public static Identifier id(final String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
