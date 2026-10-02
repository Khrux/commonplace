package com.khrux.handbook.client;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.client.atlas.ClientAtlas;
import com.khrux.handbook.client.compat.FieldGuideTab;
import com.khrux.handbook.client.gui.hud.CompassCoordinates;
import com.khrux.handbook.client.gui.screens.HandbookScreen;
import com.khrux.handbook.network.protocol.AtlasMarkersPayload;
import com.khrux.handbook.network.protocol.AtlasTilesPayload;
import com.khrux.handbook.network.protocol.NotebookPayload;
import com.khrux.handbook.world.item.HandbookItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class HandbookClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(NotebookPayload.TYPE, (payload, context) -> ClientNotebook.set(payload.pages()));
		ClientPlayNetworking.registerGlobalReceiver(AtlasTilesPayload.TYPE, (payload, context) -> ClientAtlas.receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(AtlasMarkersPayload.TYPE, (payload, context) -> ClientAtlas.receiveMarkers(payload.markers()));
		UseItemCallback.EVENT.register(HandbookClient::useHandbook);
		HandbookKeyMappings.bootstrap();
		SpyglassZoom.bootstrap();
		HandbookTabControls.bootstrap();
		HudElementRegistry.attachElementAfter(VanillaHudElements.MOB_EFFECTS, Handbook.id("compass_coordinates"), CompassCoordinates::extractRenderState);
		if (FabricLoader.getInstance().isModLoaded("fieldguide")) {
			FieldGuideTab.bootstrap();
		}
	}

	private static InteractionResult useHandbook(final Player player, final Level level, final InteractionHand hand) {
		if (!level.isClientSide() || !player.getItemInHand(hand).is(HandbookItems.HANDBOOK)) {
			return InteractionResult.PASS;
		}

		HandbookScreen.open(null);
		return InteractionResult.SUCCESS;
	}
}
