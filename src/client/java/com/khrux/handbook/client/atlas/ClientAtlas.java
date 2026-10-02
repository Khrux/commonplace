package com.khrux.handbook.client.atlas;

import com.khrux.handbook.network.protocol.AtlasTilesPayload;
import com.khrux.handbook.world.level.atlas.AtlasMarker;
import com.khrux.handbook.world.level.atlas.WorldAtlas;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public class ClientAtlas {
	private static final Long2IntMap TILES = new Long2IntOpenHashMap();
	private static final List<String> PALETTE = new ArrayList<>();
	private static final Long2ObjectMap<String> STRUCTURES = new Long2ObjectOpenHashMap<>();
	private static List<AtlasMarker> markers = List.of();
	private static int version;

	static {
		TILES.defaultReturnValue(WorldAtlas.NO_TILE);
	}

	public static void receive(final AtlasTilesPayload payload) {
		if (payload.reset()) {
			TILES.clear();
			STRUCTURES.clear();
		}

		for (int i = 0; i < payload.structurePositions().length; i++) {
			STRUCTURES.put(payload.structurePositions()[i], payload.structureMarkers().get(i));
		}

		PALETTE.clear();
		PALETTE.addAll(payload.palette());
		for (int i = 0; i < payload.positions().length; i++) {
			TILES.put(payload.positions()[i], payload.tiles()[i]);
		}

		version++;
	}

	public static int getVersion() {
		return version;
	}

	public static void receiveMarkers(final List<AtlasMarker> received) {
		markers = received;
	}

	public static Long2ObjectMap<String> getStructures() {
		return STRUCTURES;
	}

	public static List<AtlasMarker> getMarkers() {
		return markers;
	}

	public static @Nullable AtlasTileTexture getTexture(final int chunkX, final int chunkZ) {
		int tile = TILES.get(ChunkPos.pack(chunkX, chunkZ));
		if (tile == WorldAtlas.NO_TILE) {
			return null;
		}

		int provider = WorldAtlas.getProvider(tile);
		if (provider >= PALETTE.size()) {
			return null;
		}

		return AtlasTextures.get(Minecraft.getInstance().getResourceManager())
			.getTexture(PALETTE.get(provider), WorldAtlas.getElevation(tile), chunkX, chunkZ);
	}
}
