package com.khrux.handbook.client;

import com.google.common.hash.Hashing;
import com.google.gson.JsonParser;
import com.khrux.handbook.network.protocol.FollowPassphrasePayload;
import com.khrux.handbook.network.protocol.PassphraseSettingsPayload;
import com.khrux.handbook.network.protocol.PassphraseSlotsPayload;
import com.khrux.handbook.network.protocol.SharePlainInkPayload;
import com.khrux.handbook.world.entity.player.PassphraseSlot;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;

public class ClientPassphrases {
	private static final Codec<Map<String, String>> KNOWN_CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING);
	private static final Path KNOWN_PATH = FabricLoader.getInstance().getConfigDir().resolve("handbook").resolve("passphrases.json");
	private static final Map<String, String> KNOWN = new HashMap<>();
	private static List<PassphraseSlotsPayload.SlotView> slots = createEmpty();
	private static boolean loaded;
	private static int version;

	private static List<PassphraseSlotsPayload.SlotView> createEmpty() {
		List<PassphraseSlotsPayload.SlotView> empty = new ArrayList<>();
		for (PassphraseSlot slot : PassphraseSlot.createSlots()) {
			empty.add(new PassphraseSlotsPayload.SlotView(slot, List.of()));
		}

		return List.copyOf(empty);
	}

	public static void receive(final PassphraseSlotsPayload payload) {
		if (payload.slots().size() == PassphraseSlot.SLOTS) {
			boolean changed = !slots.stream().map(PassphraseSlotsPayload.SlotView::slot).toList().equals(payload.slots().stream().map(PassphraseSlotsPayload.SlotView::slot).toList());
			slots = payload.slots();
			if (changed) {
				version++;
				ClientSheen.changed();
			}
		}
	}

	public static void reset() {
		slots = createEmpty();
		version++;
	}

	public static int getVersion() {
		return version;
	}

	public static PassphraseSlotsPayload.SlotView get(final int index) {
		return slots.get(index);
	}

	public static String hash(final String passphrase) {
		return Hashing.sha256().hashString(passphrase, StandardCharsets.UTF_8).toString();
	}

	public static @Nullable String getPassphrase(final String hash) {
		load();
		return KNOWN.get(hash);
	}

	public static void follow(final int index, final String passphrase) {
		if (passphrase.isEmpty()) {
			ClientPlayNetworking.send(new FollowPassphrasePayload(index, ""));
			return;
		}

		String hash = hash(passphrase);
		load();
		if (!passphrase.equals(KNOWN.put(hash, passphrase))) {
			save();
		}

		ClientPlayNetworking.send(new FollowPassphrasePayload(index, hash));
	}

	public static void changeSettings(final int index, final int color, final boolean enderInk) {
		ClientPlayNetworking.send(new PassphraseSettingsPayload(index, color, enderInk));
	}

	public static void sharePlainInk(final int index) {
		ClientPlayNetworking.send(new SharePlainInkPayload(index));
	}

	private static void load() {
		if (loaded) {
			return;
		}

		loaded = true;
		if (!Files.isRegularFile(KNOWN_PATH)) {
			return;
		}

		try {
			KNOWN_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(KNOWN_PATH))).ifSuccess(KNOWN::putAll);
		} catch (IOException | RuntimeException ignored) {
		}
	}

	private static void save() {
		KNOWN_CODEC.encodeStart(JsonOps.INSTANCE, KNOWN).ifSuccess(json -> {
			try {
				Files.createDirectories(KNOWN_PATH.getParent());
				Files.writeString(KNOWN_PATH, json.toString());
			} catch (IOException ignored) {
			}
		});
	}
}
