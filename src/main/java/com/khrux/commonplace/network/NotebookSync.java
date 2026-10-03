package com.khrux.commonplace.network;

import com.khrux.commonplace.CommonplaceConfig;
import com.khrux.commonplace.network.protocol.NoteEditPayload;
import com.khrux.commonplace.network.protocol.NoteOptionsPayload;
import com.khrux.commonplace.network.protocol.NotebookPayload;
import com.khrux.commonplace.world.entity.player.CommonplaceAttachmentTypes;
import com.khrux.commonplace.world.entity.player.NotePage;
import com.khrux.commonplace.world.level.syndicate.PassphraseNotebooks;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public class NotebookSync {
	public static void bootstrap() {
		PayloadTypeRegistry.clientboundPlay().register(NotebookPayload.TYPE, NotebookPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(NoteOptionsPayload.TYPE, NoteOptionsPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(NoteEditPayload.TYPE, NoteEditPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(NoteEditPayload.TYPE, NoteEditPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(NoteEditPayload.TYPE, (payload, context) -> edit(context.player(), payload));
		ServerLifecycleEvents.SERVER_STARTING.register(server -> CommonplaceConfig.load());
		ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> send(listener.player));
	}

	private static void edit(final ServerPlayer player, final NoteEditPayload payload) {
		if (payload.book() != NoteEditPayload.PERSONAL) {
			PassphraseNotebooks.edit(player, payload.book(), payload.page(), payload.pixels(), payload.text());
			return;
		}

		if (payload.page() < 0 || payload.page() >= NotePage.PAGES || payload.text().filter(text -> text.length() > NotePage.MAX_TEXT_LENGTH).isPresent()) {
			return;
		}

		List<NotePage> pages = new ArrayList<>(player.getAttachedOrElse(CommonplaceAttachmentTypes.NOTEBOOK, List.of()));
		while (pages.size() <= payload.page()) {
			pages.add(NotePage.EMPTY);
		}

		NotePage updated = pages.get(payload.page()).withPixels(payload.pixels());
		if (payload.text().isPresent()) {
			updated = updated.withText(payload.text().get());
		}

		pages.set(payload.page(), updated);
		player.setAttached(CommonplaceAttachmentTypes.NOTEBOOK, List.copyOf(pages));
	}

	private static void send(final ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, NoteOptionsPayload.TYPE)) {
			ServerPlayNetworking.send(player, new NoteOptionsPayload(CommonplaceConfig.getNoteOptions()));
		}

		if (ServerPlayNetworking.canSend(player, NotebookPayload.TYPE)) {
			ServerPlayNetworking.send(player, new NotebookPayload(NoteEditPayload.PERSONAL, player.getAttachedOrElse(CommonplaceAttachmentTypes.NOTEBOOK, List.of())));
		}
	}
}
