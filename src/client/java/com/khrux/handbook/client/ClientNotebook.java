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
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientNotebook {
	private final Map<Integer, List<NotePage>> books = new HashMap<>();
	private final Map<ClientNotebook.PageKey, IntArrayList> pendingPixels = new LinkedHashMap<>();
	private final Map<ClientNotebook.PageKey, String> pendingText = new LinkedHashMap<>();
	private HandbookConfig.NoteOptions options = HandbookConfig.NoteOptions.DEFAULT;

	public HandbookConfig.NoteOptions getOptions() {
		return this.options;
	}

	public void setOptions(final HandbookConfig.NoteOptions options) {
		this.options = options;
	}

	public void set(final NotebookPayload payload) {
		this.books.put(payload.book(), new ArrayList<>(payload.pages()));
	}

	public int getPageCount(final int book) {
		return book == NoteEditPayload.PERSONAL ? NotePage.PAGES : this.options.passphrasePages();
	}

	public NotePage get(final int book, final int page) {
		List<NotePage> pages = this.books.get(book);
		return pages != null && page < pages.size() ? pages.get(page) : NotePage.EMPTY;
	}

	private void put(final int book, final int page, final NotePage notePage) {
		List<NotePage> pages = this.books.computeIfAbsent(book, key -> new ArrayList<>());
		while (pages.size() <= page) {
			pages.add(NotePage.EMPTY);
		}

		pages.set(page, notePage);
	}

	public void paint(final int book, final int page, final int x, final int y, final int color) {
		int pixel = NoteEditPayload.pack(x, y, color);
		this.put(book, page, this.get(book, page).withPixels(new int[]{pixel}));
		this.pendingPixels.computeIfAbsent(new ClientNotebook.PageKey(book, page), key -> new IntArrayList()).add(pixel);
	}

	public void setText(final int book, final int page, final String text) {
		this.put(book, page, this.get(book, page).withText(text));
		this.pendingText.put(new ClientNotebook.PageKey(book, page), text);
	}

	public void receive(final NoteEditPayload payload) {
		NotePage updated = this.get(payload.book(), payload.page()).withPixels(payload.pixels());
		if (payload.text().isPresent()) {
			updated = updated.withText(payload.text().get());
		}

		this.put(payload.book(), payload.page(), updated);
	}

	public void flush() {
		if (!ClientPlayNetworking.canSend(NoteEditPayload.TYPE)) {
			this.pendingPixels.clear();
			this.pendingText.clear();
			return;
		}

		this.pendingPixels.forEach((key, pixels) -> {
			int[] all = pixels.toIntArray();
			for (int i = 0; i < all.length; i += NoteEditPayload.MAX_PIXELS) {
				Optional<String> text = i == 0 ? Optional.ofNullable(this.pendingText.remove(key)) : Optional.empty();
				ClientPlayNetworking.send(new NoteEditPayload(key.book(), key.page(), Arrays.copyOfRange(all, i, Math.min(i + NoteEditPayload.MAX_PIXELS, all.length)), text));
			}
		});
		this.pendingText.forEach((key, text) -> ClientPlayNetworking.send(new NoteEditPayload(key.book(), key.page(), new int[0], Optional.of(text))));
		this.pendingPixels.clear();
		this.pendingText.clear();
	}

	private record PageKey(int book, int page) {
	}
}
