package com.khrux.handbook.network;

import com.khrux.handbook.network.protocol.NotePagePayload;
import com.khrux.handbook.network.protocol.NotebookPayload;
import com.khrux.handbook.world.entity.player.HandbookAttachmentTypes;
import com.khrux.handbook.world.entity.player.NotePage;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public class NotebookSync {
	public static void bootstrap() {
		PayloadTypeRegistry.clientboundPlay().register(NotebookPayload.TYPE, NotebookPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(NotePagePayload.TYPE, NotePagePayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(NotePagePayload.TYPE, (payload, context) -> update(context.player(), payload));
		ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> send(listener.player));
	}

	private static void update(final ServerPlayer player, final NotePagePayload payload) {
		if (payload.page() < 0 || payload.page() >= NotePage.PAGES || !payload.notePage().isValid()) {
			return;
		}

		List<NotePage> pages = new ArrayList<>(player.getAttachedOrElse(HandbookAttachmentTypes.NOTEBOOK, List.of()));
		while (pages.size() <= payload.page()) {
			pages.add(NotePage.EMPTY);
		}

		pages.set(payload.page(), payload.notePage());
		player.setAttached(HandbookAttachmentTypes.NOTEBOOK, List.copyOf(pages));
	}

	private static void send(final ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, NotebookPayload.TYPE)) {
			ServerPlayNetworking.send(player, new NotebookPayload(player.getAttachedOrElse(HandbookAttachmentTypes.NOTEBOOK, List.of())));
		}
	}
}
