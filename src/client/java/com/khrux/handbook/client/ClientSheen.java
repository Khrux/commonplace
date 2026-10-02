package com.khrux.handbook.client;

import com.khrux.handbook.client.gui.screens.EnderPage;
import com.khrux.handbook.client.gui.screens.HandbookTab;
import com.khrux.handbook.network.protocol.SheenPayload;
import com.khrux.handbook.world.entity.player.PassphraseSlot;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.util.ARGB;

public class ClientSheen {
	public static final int NONE = 0;
	public static final int DIMMED = 1;
	public static final int LEFT_COLOR = 0xFF8E8A84;
	private static final ClientSheen.Layer[] LAYERS = new ClientSheen.Layer[SheenPayload.LEFT_LAYER + 1];
	private static int highlighted = -1;
	private static int entryVersion;
	private static int version;

	static {
		reset();
	}

	public static void reset() {
		for (int i = 0; i < LAYERS.length; i++) {
			LAYERS[i] = new ClientSheen.Layer();
		}

		highlighted = -1;
		entryVersion++;
		version++;
	}

	public static void receive(final SheenPayload payload) {
		if (payload.reset()) {
			for (int i = 0; i < LAYERS.length; i++) {
				LAYERS[i] = new ClientSheen.Layer();
			}
		}

		if (payload.layer() < 0 || payload.layer() >= LAYERS.length) {
			return;
		}

		ClientSheen.Layer layer = LAYERS[payload.layer()];
		layer.recipes.addAll(payload.recipes());
		for (String entry : payload.entries()) {
			layer.entries.add(iconKey(entry));
		}

		version++;
		if (payload.reset() || !payload.entries().isEmpty()) {
			entryVersion++;
		version++;
		}

		if (payload.chunks().length > 0) {
			LongSet chunks = layer.chunks.computeIfAbsent(payload.dimension(), key -> new LongOpenHashSet());
			for (long pos : payload.chunks()) {
				chunks.add(pos);
			}
		}
	}

	public static int getVersion() {
		return version;
	}

	public static int getEntryVersion() {
		return entryVersion;
	}

	public static void changed() {
		entryVersion++;
		version++;
	}

	public static int getHighlighted() {
		return HandbookTab.ENDER.isAvailable() ? highlighted : -1;
	}

	public static void toggleHighlight(final int slot) {
		highlighted = highlighted == slot ? -1 : slot;
		entryVersion++;
		version++;
	}

	public static int getRecipeSheen(final int display) {
		return sheen(layer -> layer.recipes.contains(display));
	}

	public static int getChunkSheen(final String dimension, final long pos) {
		return sheen(layer -> {
			LongSet chunks = layer.chunks.get(dimension);
			return chunks != null && chunks.contains(pos);
		});
	}

	public static int getIconSheen(final String iconKey) {
		return sheen(layer -> {
			for (int end = iconKey.lastIndexOf('_'); end > 0; end = iconKey.lastIndexOf('_', end - 1)) {
				if (layer.entries.contains(iconKey.substring(0, end))) {
					return true;
				}
			}

			return false;
		});
	}

	private static String iconKey(final String entry) {
		int variantStart = entry.indexOf('#');
		String base = variantStart < 0 ? entry : entry.substring(0, variantStart);
		return base.replace(":", "_").replace("/", "_").toLowerCase(Locale.ROOT);
	}

	private static int sheen(final Predicate<ClientSheen.Layer> contains) {
		int highlight = getHighlighted();
		if (highlight >= 0) {
			PassphraseSlot slot = ClientPassphrases.get(highlight).slot();
			return !slot.isEmpty() && contains.test(LAYERS[highlight]) ? ARGB.opaque(EnderPage.getSlotColor(slot)) : DIMMED;
		}

		for (int i = 0; i < PassphraseSlot.SLOTS; i++) {
			PassphraseSlot slot = ClientPassphrases.get(i).slot();
			if (!slot.isEmpty() && contains.test(LAYERS[i])) {
				return ARGB.opaque(EnderPage.getSlotColor(slot));
			}
		}

		return contains.test(LAYERS[SheenPayload.LEFT_LAYER]) ? LEFT_COLOR : NONE;
	}

	private static class Layer {
		private final IntSet recipes = new IntOpenHashSet();
		private final Set<String> entries = new HashSet<>();
		private final Map<String, LongSet> chunks = new HashMap<>();
	}
}
