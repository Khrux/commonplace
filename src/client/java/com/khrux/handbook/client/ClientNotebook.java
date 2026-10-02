package com.khrux.handbook.client;

import com.khrux.handbook.network.protocol.NotePagePayload;
import com.khrux.handbook.world.entity.player.NotePage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientNotebook {
	private static final List<NotePage> PAGES = new ArrayList<>();
	private static final Set<Integer> CHANGED = new TreeSet<>();

	public static void set(final List<NotePage> pages) {
		PAGES.clear();
		PAGES.addAll(pages);
		CHANGED.clear();
	}

	public static NotePage get(final int page) {
		return page < PAGES.size() ? PAGES.get(page) : NotePage.EMPTY;
	}

	public static void put(final int page, final NotePage notePage) {
		while (PAGES.size() <= page) {
			PAGES.add(NotePage.EMPTY);
		}

		PAGES.set(page, notePage);
		CHANGED.add(page);
	}

	public static void send() {
		if (ClientPlayNetworking.canSend(NotePagePayload.TYPE)) {
			for (int page : CHANGED) {
				ClientPlayNetworking.send(new NotePagePayload(page, PAGES.get(page)));
			}
		}

		CHANGED.clear();
	}
}
