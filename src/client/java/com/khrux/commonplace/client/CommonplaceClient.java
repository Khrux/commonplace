package com.khrux.commonplace.client;

import com.khrux.commonplace.Commonplace;
import com.khrux.commonplace.CommonplaceConfig;
import com.khrux.commonplace.client.atlas.ClientAtlas;
import com.khrux.commonplace.client.compat.FieldGuideTab;
import com.khrux.commonplace.client.compat.PunchyCompat;
import com.khrux.commonplace.client.gui.hud.CompassCoordinates;
import com.khrux.commonplace.client.gui.screens.HandbookScreen;
import com.khrux.commonplace.client.renderer.InkMasks;
import com.khrux.commonplace.client.renderer.PageSnapshot;
import com.khrux.commonplace.network.protocol.AtlasMarkersPayload;
import com.khrux.commonplace.network.protocol.AtlasTilesPayload;
import com.khrux.commonplace.network.protocol.NoteEditPayload;
import com.khrux.commonplace.network.protocol.NoteOptionsPayload;
import com.khrux.commonplace.network.protocol.NotebookPayload;
import com.khrux.commonplace.network.protocol.PassphraseSlotsPayload;
import com.khrux.commonplace.network.protocol.SheenPayload;
import com.khrux.commonplace.world.item.CommonplaceItems;
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

public class CommonplaceClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		CommonplaceConfig.load();
		ClientPlayNetworking.registerGlobalReceiver(NotebookPayload.TYPE, (payload, context) -> ClientHandbook.get().notebook().set(payload));
		ClientPlayNetworking.registerGlobalReceiver(NoteEditPayload.TYPE, (payload, context) -> ClientHandbook.get().notebook().receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(NoteOptionsPayload.TYPE, (payload, context) -> ClientHandbook.get().notebook().setOptions(payload.options()));
		ClientHandbook.bootstrap();
		PageSnapshot.bootstrap();
		ClientPlayNetworking.registerGlobalReceiver(AtlasTilesPayload.TYPE, (payload, context) -> ClientHandbook.get().atlas().receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(AtlasMarkersPayload.TYPE, (payload, context) -> ClientHandbook.get().atlas().receiveMarkers(payload.markers()));
		ClientPlayNetworking.registerGlobalReceiver(PassphraseSlotsPayload.TYPE, (payload, context) -> ClientHandbook.get().passphrases().receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(SheenPayload.TYPE, (payload, context) -> ClientHandbook.get().sheen().receive(payload));
		UseItemCallback.EVENT.register(CommonplaceClient::useHandbook);
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Commonplace.id("ink_masks"), (ResourceManagerReloadListener)resourceManager -> InkMasks.clear());
		CommonplaceKeyMappings.bootstrap();
		SpyglassZoom.bootstrap();
		HandbookTabControls.bootstrap();
		HudElementRegistry.attachElementAfter(VanillaHudElements.MOB_EFFECTS, Commonplace.id("compass_coordinates"), CompassCoordinates::extractRenderState);
		if (FabricLoader.getInstance().isModLoaded("fieldguide")) {
			FieldGuideTab.bootstrap();
		}

		if (FabricLoader.getInstance().isModLoaded("punchy")) {
			PunchyCompat.bootstrap();
		}
	}

	private static InteractionResult useHandbook(final Player player, final Level level, final InteractionHand hand) {
		if (!level.isClientSide() || !player.getItemInHand(hand).is(CommonplaceItems.HANDBOOK)) {
			return InteractionResult.PASS;
		}

		HandbookScreen.open(null);
		return InteractionResult.SUCCESS;
	}
}
