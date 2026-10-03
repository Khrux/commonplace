package com.khrux.handbook.world.level.syndicate;

import com.khrux.handbook.HandbookConfig;
import com.khrux.handbook.network.protocol.NoteEditPayload;
import com.khrux.handbook.network.protocol.NotebookPayload;
import com.khrux.handbook.world.entity.player.NotePage;
import com.khrux.handbook.world.entity.player.PassphraseSlot;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class PassphraseNotebooks {
	private static final int FADE_INTERVAL = 1200;
	private static final long TICKS_PER_DAY = 24000L;
	private static final int BLACK_INK = 15;

	public static void edit(final ServerPlayer player, final int index, final int page, final int[] pixels, final Optional<String> text) {
		HandbookConfig.NoteOptions options = HandbookConfig.getNoteOptions();
		if (index < 0 || index >= PassphraseSlot.SLOTS || page < 0 || page >= options.passphrasePages() || !Syndication.isActive(player)) {
			return;
		}

		PassphraseSlot slot = Syndication.getSlots(player).get(index);
		Syndicate syndicate = slot.isEmpty() ? null : Syndication.getSyndicates(player.level().getServer()).get(slot.hash());
		if (syndicate == null || !syndicate.getFollowers().containsKey(player.getUUID())) {
			return;
		}

		int[] allowed = allowedPixels(pixels, options.colors());
		Optional<String> allowedText = text.filter(value -> value.length() <= NotePage.MAX_TEXT_LENGTH);
		if (allowed.length == 0 && allowedText.isEmpty()) {
			return;
		}

		List<NotePage> notes = syndicate.getNotes();
		while (notes.size() <= page) {
			notes.add(NotePage.EMPTY);
		}

		NotePage updated = notes.get(page).withPixels(allowed);
		if (allowedText.isPresent()) {
			updated = updated.withText(allowedText.get());
		}

		notes.set(page, updated);
		MinecraftServer server = player.level().getServer();
		for (UUID uuid : syndicate.getFollowers().keySet()) {
			ServerPlayer follower = server.getPlayerList().getPlayer(uuid);
			if (follower == null || !Syndication.isActive(follower) || !ServerPlayNetworking.canSend(follower, NoteEditPayload.TYPE)) {
				continue;
			}

			List<PassphraseSlot> slots = Syndication.getSlots(follower);
			for (int i = 0; i < slots.size(); i++) {
				if (slots.get(i).hash().equals(slot.hash())) {
					ServerPlayNetworking.send(follower, new NoteEditPayload(i, page, allowed, allowedText));
				}
			}
		}
	}

	static int[] allowedPixels(final int[] pixels, final HandbookConfig.NoteColors colors) {
		if (colors == HandbookConfig.NoteColors.TEXT) {
			return new int[0];
		}

		if (colors == HandbookConfig.NoteColors.FULL) {
			return pixels;
		}

		int[] black = new int[pixels.length];
		for (int i = 0; i < pixels.length; i++) {
			int pixel = pixels[i];
			black[i] = NoteEditPayload.getColor(pixel) == 0 ? pixel : NoteEditPayload.pack(NoteEditPayload.getX(pixel), NoteEditPayload.getY(pixel), BLACK_INK);
		}

		return black;
	}

	public static void send(final ServerPlayer player, final int index) {
		if (!ServerPlayNetworking.canSend(player, NotebookPayload.TYPE)) {
			return;
		}

		PassphraseSlot slot = Syndication.getSlots(player).get(index);
		Syndicate syndicate = slot.isEmpty() ? null : Syndication.getSyndicates(player.level().getServer()).get(slot.hash());
		List<NotePage> notes = syndicate == null ? List.of() : syndicate.getNotes();
		int pages = Math.min(notes.size(), HandbookConfig.getNoteOptions().passphrasePages());
		ServerPlayNetworking.send(player, new NotebookPayload(index, List.copyOf(notes.subList(0, pages))));
	}

	public static void sendAll(final ServerPlayer player) {
		for (int i = 0; i < PassphraseSlot.SLOTS; i++) {
			send(player, i);
		}
	}

	static void abandon(final Syndicate syndicate, final long gameTime) {
		if (syndicate.getFollowers().isEmpty() && syndicate.getAbandonedAt() == Syndicate.FOLLOWED) {
			syndicate.setAbandonedAt(gameTime);
		}
	}

	static void rejoin(final Syndicate syndicate, final long gameTime) {
		if (syndicate.getAbandonedAt() != Syndicate.FOLLOWED) {
			fade(syndicate, gameTime);
			syndicate.setAbandonedAt(Syndicate.FOLLOWED);
		}
	}

	static void tick(final MinecraftServer server) {
		if (server.getTickCount() % FADE_INTERVAL != 0 || HandbookConfig.getNoteOptions().fadeDays() == 0) {
			return;
		}

		long gameTime = server.overworld().getGameTime();
		for (Syndicate syndicate : Syndication.getSyndicates(server).getAll()) {
			if (syndicate.getAbandonedAt() != Syndicate.FOLLOWED && !syndicate.getNotes().isEmpty()) {
				fade(syndicate, gameTime);
			}
		}
	}

	static void fade(final Syndicate syndicate, final long gameTime) {
		int fadeDays = HandbookConfig.getNoteOptions().fadeDays();
		if (fadeDays == 0 || syndicate.getAbandonedAt() == Syndicate.FOLLOWED) {
			return;
		}

		float progress = (float)(gameTime - syndicate.getAbandonedAt()) / (fadeDays * TICKS_PER_DAY);
		List<NotePage> notes = syndicate.getNotes();
		if (progress >= 1.0F) {
			notes.clear();
			return;
		}

		for (int page = 0; page < notes.size(); page++) {
			notes.set(page, fadePage(notes.get(page), page, progress));
		}
	}

	static NotePage fadePage(final NotePage notePage, final int page, final float progress) {
		int[] erased = new int[NotePage.CANVAS_WIDTH * NotePage.CANVAS_HEIGHT];
		int count = 0;
		for (int y = 0; y < NotePage.CANVAS_HEIGHT; y++) {
			for (int x = 0; x < NotePage.CANVAS_WIDTH; x++) {
				if (notePage.getPixel(x, y) != 0 && fadeRoll(page, y * NotePage.CANVAS_WIDTH + x) < progress) {
					erased[count++] = NoteEditPayload.pack(x, y, 0);
				}
			}
		}

		StringBuilder text = new StringBuilder(notePage.text());
		for (int i = 0; i < text.length(); i++) {
			if (!Character.isWhitespace(text.charAt(i)) && fadeRoll(page + NotePage.PAGES, i) < progress) {
				text.setCharAt(i, ' ');
			}
		}

		return notePage.withPixels(Arrays.copyOf(erased, count)).withText(text.toString());
	}

	private static float fadeRoll(final int page, final int index) {
		int hash = page * 374761393 + index * 668265263;
		hash = (hash ^ hash >>> 13) * 1274126177;
		return ((hash ^ hash >>> 16) & 0xFFFFFF) / 16777216.0F;
	}
}
