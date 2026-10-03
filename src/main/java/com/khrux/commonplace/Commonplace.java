package com.khrux.commonplace;

import com.khrux.commonplace.compat.FieldGuideCompat;
import com.khrux.commonplace.network.NotebookSync;
import com.khrux.commonplace.world.entity.player.CommonplaceAttachmentTypes;
import com.khrux.commonplace.world.inventory.ToolSlot;
import com.khrux.commonplace.world.item.HandbookItem;
import com.khrux.commonplace.world.item.CommonplaceItems;
import com.khrux.commonplace.world.level.atlas.AtlasTracker;
import com.khrux.commonplace.world.level.syndicate.Syndication;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

public class Commonplace implements ModInitializer {
	public static final String MOD_ID = "commonplace";

	@Override
	public void onInitialize() {
		CommonplaceItems.bootstrap();
		CommonplaceAttachmentTypes.bootstrap();
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
