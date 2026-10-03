package com.khrux.commonplace.client;

import com.khrux.commonplace.CommonplaceConfig;
import com.khrux.commonplace.client.gui.screens.EnderPage;
import com.khrux.commonplace.client.gui.screens.HandbookTab;
import com.khrux.commonplace.network.protocol.SheenPayload;
import com.khrux.commonplace.world.entity.player.PassphraseSlot;
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
	private final ClientSheen.Layer[] layers = new ClientSheen.Layer[SheenPayload.LEFT_LAYER + 1];
	private int highlighted = -1;
	private int entryVersion;
	private int version;

	public ClientSheen() {
		this.clearLayers();
	}

	private void clearLayers() {
		for (int i = 0; i < this.layers.length; i++) {
			this.layers[i] = new ClientSheen.Layer();
		}
	}

	public void receive(final SheenPayload payload) {
		if (payload.reset()) {
			this.clearLayers();
		}

		if (payload.layer() < 0 || payload.layer() >= this.layers.length) {
			return;
		}

		ClientSheen.Layer layer = this.layers[payload.layer()];
		layer.recipes.addAll(payload.recipes());
		for (String entry : payload.entries()) {
			layer.entries.add(iconKey(entry));
		}

		if (payload.chunks().length > 0) {
			LongSet chunks = layer.chunks.computeIfAbsent(payload.dimension(), key -> new LongOpenHashSet());
			for (long pos : payload.chunks()) {
				chunks.add(pos);
			}
		}

		this.version++;
		if (payload.reset() || !payload.entries().isEmpty()) {
			this.entryVersion++;
		}
	}

	public int getVersion() {
		return this.version;
	}

	public int getEntryVersion() {
		return this.entryVersion;
	}

	public void changed() {
		this.entryVersion++;
		this.version++;
	}

	public int getHighlighted() {
		return HandbookTab.ENDER.isAvailable() ? this.highlighted : -1;
	}

	public void toggleHighlight(final int slot) {
		this.highlighted = this.highlighted == slot ? -1 : slot;
		this.changed();
	}

	public int getRecipeSheen(final int display) {
		return this.sheen(layer -> layer.recipes.contains(display));
	}

	public int getChunkSheen(final String dimension, final long pos) {
		if (this.isContentSheenHidden()) {
			return NONE;
		}

		return this.sheen(layer -> {
			LongSet chunks = layer.chunks.get(dimension);
			return chunks != null && chunks.contains(pos);
		});
	}

	public int getIconSheen(final String iconKey) {
		if (this.isContentSheenHidden()) {
			return NONE;
		}

		return this.sheen(layer -> {
			for (int end = iconKey.lastIndexOf('_'); end > 0; end = iconKey.lastIndexOf('_', end - 1)) {
				if (layer.entries.contains(iconKey.substring(0, end))) {
					return true;
				}
			}

			return false;
		});
	}

	private boolean isContentSheenHidden() {
		return CommonplaceConfig.get().contentSheen == CommonplaceConfig.ContentSheen.HIGHLIGHTED && this.getHighlighted() < 0;
	}

	private static String iconKey(final String entry) {
		int variantStart = entry.indexOf('#');
		String base = variantStart < 0 ? entry : entry.substring(0, variantStart);
		return base.replace(":", "_").replace("/", "_").toLowerCase(Locale.ROOT);
	}

	private int sheen(final Predicate<ClientSheen.Layer> contains) {
		ClientPassphrases passphrases = ClientHandbook.get().passphrases();
		int highlight = this.getHighlighted();
		if (highlight >= 0) {
			PassphraseSlot slot = passphrases.get(highlight).slot();
			return !slot.isEmpty() && contains.test(this.layers[highlight]) ? ARGB.opaque(EnderPage.getSlotColor(slot)) : DIMMED;
		}

		for (int i = 0; i < PassphraseSlot.SLOTS; i++) {
			PassphraseSlot slot = passphrases.get(i).slot();
			if (!slot.isEmpty() && contains.test(this.layers[i])) {
				return ARGB.opaque(EnderPage.getSlotColor(slot));
			}
		}

		return contains.test(this.layers[SheenPayload.LEFT_LAYER]) ? LEFT_COLOR : NONE;
	}

	private static class Layer {
		private final IntSet recipes = new IntOpenHashSet();
		private final Set<String> entries = new HashSet<>();
		private final Map<String, LongSet> chunks = new HashMap<>();
	}
}
