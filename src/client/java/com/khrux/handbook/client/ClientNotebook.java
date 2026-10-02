package com.khrux.handbook.client;

import com.khrux.handbook.HandbookConfig;
import com.khrux.handbook.network.protocol.NoteEditPayload;
import com.khrux.handbook.network.protocol.NotebookPayload;
import com.khrux.handbook.world.entity.player.NotePage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientNotebook {
	private static final int SEND_INTERVAL = 2;
	private static final Map<Integer, List<NotePage>> BOOKS = new HashMap<>();
	private static final Map<ClientNotebook.PageKey, IntArrayList> PENDING_PIXELS = new LinkedHashMap<>();
	private static final Map<ClientNotebook.PageKey, String> PENDING_TEXT = new LinkedHashMap<>();
	private static HandbookConfig.NoteOptions options = HandbookConfig.NoteOptions.DEFAULT;
	private static int ticks;

	public static void bootstrap() {
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			if (++ticks % SEND_INTERVAL == 0) {
				flush();
			}
		});
	}

	public static void reset() {
		BOOKS.clear();
		PENDING_PIXELS.clear();
		PENDING_TEXT.clear();
		options = HandbookConfig.NoteOptions.DEFAULT;
	}

	public static HandbookConfig.NoteOptions getOptions() {
		return options;
	}

	public static void setOptions(final HandbookConfig.NoteOptions newOptions) {
		options = newOptions;
	}

	public static void set(final NotebookPayload payload) {
		BOOKS.put(payload.book(), new ArrayList<>(payload.pages()));
	}

	public static int getPageCount(final int book) {
		return book == NoteEditPayload.PERSONAL ? NotePage.PAGES : options.passphrasePages();
	}

	public static NotePage get(final int book, final int page) {
		List<NotePage> pages = BOOKS.get(book);
		return pages != null && page < pages.size() ? pages.get(page) : NotePage.EMPTY;
	}

	private static void put(final int book, final int page, final NotePage notePage) {
		List<NotePage> pages = BOOKS.computeIfAbsent(book, key -> new ArrayList<>());
		while (pages.size() <= page) {
			pages.add(NotePage.EMPTY);
		}

		pages.set(page, notePage);
	}

	public static void paint(final int book, final int page, final int x, final int y, final int color) {
		int pixel = NoteEditPayload.pack(x, y, color);
		put(book, page, get(book, page).withPixels(new int[]{pixel}));
		PENDING_PIXELS.computeIfAbsent(new ClientNotebook.PageKey(book, page), key -> new IntArrayList()).add(pixel);
	}

	public static void setText(final int book, final int page, final String text) {
		put(book, page, get(book, page).withText(text));
		PENDING_TEXT.put(new ClientNotebook.PageKey(book, page), text);
	}

	public static void receive(final NoteEditPayload payload) {
		NotePage updated = get(payload.book(), payload.page()).withPixels(payload.pixels());
		put(payload.book(), payload.page(), payload.text().map(updated::withText).orElse(updated));
	}

	public static void flush() {
		if (!ClientPlayNetworking.canSend(NoteEditPayload.TYPE)) {
			PENDING_PIXELS.clear();
			PENDING_TEXT.clear();
			return;
		}

		PENDING_PIXELS.forEach((key, pixels) -> {
			int[] all = pixels.toIntArray();
			for (int i = 0; i < all.length; i += NoteEditPayload.MAX_PIXELS) {
				Optional<String> text = i == 0 ? Optional.ofNullable(PENDING_TEXT.remove(key)) : Optional.empty();
				ClientPlayNetworking.send(new NoteEditPayload(key.book(), key.page(), Arrays.copyOfRange(all, i, Math.min(i + NoteEditPayload.MAX_PIXELS, all.length)), text));
			}
		});
		PENDING_TEXT.forEach((key, text) -> ClientPlayNetworking.send(new NoteEditPayload(key.book(), key.page(), new int[0], Optional.of(text))));
		PENDING_PIXELS.clear();
		PENDING_TEXT.clear();
	}

	private record PageKey(int book, int page) {
	}
}
