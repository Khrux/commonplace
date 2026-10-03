package com.khrux.handbook.client;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.HandbookConfig;
import com.khrux.handbook.client.atlas.ClientAtlas;
import com.khrux.handbook.client.compat.FieldGuideTab;
import com.khrux.handbook.client.gui.hud.CompassCoordinates;
import com.khrux.handbook.client.gui.screens.HandbookScreen;
import com.khrux.handbook.client.renderer.InkMasks;
import com.khrux.handbook.client.renderer.PageSnapshot;
import com.khrux.handbook.network.protocol.AtlasMarkersPayload;
import com.khrux.handbook.network.protocol.AtlasTilesPayload;
import com.khrux.handbook.network.protocol.NoteEditPayload;
import com.khrux.handbook.network.protocol.NoteOptionsPayload;
import com.khrux.handbook.network.protocol.NotebookPayload;
import com.khrux.handbook.network.protocol.PassphraseSlotsPayload;
import com.khrux.handbook.network.protocol.SheenPayload;
import com.khrux.handbook.world.item.HandbookItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class HandbookClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HandbookConfig.load();
		ClientPlayNetworking.registerGlobalReceiver(NotebookPayload.TYPE, (payload, context) -> ClientHandbook.get().notebook().set(payload));
		ClientPlayNetworking.registerGlobalReceiver(NoteEditPayload.TYPE, (payload, context) -> ClientHandbook.get().notebook().receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(NoteOptionsPayload.TYPE, (payload, context) -> ClientHandbook.get().notebook().setOptions(payload.options()));
		ClientHandbook.bootstrap();
		PageSnapshot.bootstrap();
		ClientPlayNetworking.registerGlobalReceiver(AtlasTilesPayload.TYPE, (payload, context) -> ClientHandbook.get().atlas().receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(AtlasMarkersPayload.TYPE, (payload, context) -> ClientHandbook.get().atlas().receiveMarkers(payload.markers()));
		ClientPlayNetworking.registerGlobalReceiver(PassphraseSlotsPayload.TYPE, (payload, context) -> ClientHandbook.get().passphrases().receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(SheenPayload.TYPE, (payload, context) -> ClientHandbook.get().sheen().receive(payload));
		UseItemCallback.EVENT.register(HandbookClient::useHandbook);
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Handbook.id("ink_masks"), (ResourceManagerReloadListener)resourceManager -> InkMasks.clear());
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
