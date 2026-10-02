package com.khrux.handbook.network;

import com.khrux.handbook.HandbookConfig;
import com.khrux.handbook.network.protocol.NoteEditPayload;
import com.khrux.handbook.network.protocol.NoteOptionsPayload;
import com.khrux.handbook.network.protocol.NotebookPayload;
import com.khrux.handbook.world.entity.player.HandbookAttachmentTypes;
import com.khrux.handbook.world.entity.player.NotePage;
import com.khrux.handbook.world.level.syndicate.PassphraseNotebooks;
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
		ServerLifecycleEvents.SERVER_STARTING.register(server -> HandbookConfig.load());
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

		List<NotePage> pages = new ArrayList<>(player.getAttachedOrElse(HandbookAttachmentTypes.NOTEBOOK, List.of()));
		while (pages.size() <= payload.page()) {
			pages.add(NotePage.EMPTY);
		}

		NotePage updated = pages.get(payload.page()).withPixels(payload.pixels());
		pages.set(payload.page(), payload.text().map(updated::withText).orElse(updated));
		player.setAttached(HandbookAttachmentTypes.NOTEBOOK, List.copyOf(pages));
	}

	private static void send(final ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, NoteOptionsPayload.TYPE)) {
			ServerPlayNetworking.send(player, new NoteOptionsPayload(HandbookConfig.getNoteOptions()));
		}

		if (ServerPlayNetworking.canSend(player, NotebookPayload.TYPE)) {
			ServerPlayNetworking.send(player, new NotebookPayload(NoteEditPayload.PERSONAL, player.getAttachedOrElse(HandbookAttachmentTypes.NOTEBOOK, List.of())));
		}
	}
}
