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
	private final Long2IntMap tiles = new Long2IntOpenHashMap();
	private final List<String> palette = new ArrayList<>();
	private final Long2ObjectMap<String> structures = new Long2ObjectOpenHashMap<>();
	private List<AtlasMarker> markers = List.of();
	private int version;

	public ClientAtlas() {
		this.tiles.defaultReturnValue(WorldAtlas.NO_TILE);
	}

	public void receive(final AtlasTilesPayload payload) {
		if (payload.reset()) {
			this.tiles.clear();
			this.structures.clear();
		}

		for (int i = 0; i < payload.structurePositions().length; i++) {
			this.structures.put(payload.structurePositions()[i], payload.structureMarkers().get(i));
		}

		this.palette.clear();
		this.palette.addAll(payload.palette());
		for (int i = 0; i < payload.positions().length; i++) {
			this.tiles.put(payload.positions()[i], payload.tiles()[i]);
		}

		this.version++;
	}

	public int getVersion() {
		return this.version;
	}

	public void receiveMarkers(final List<AtlasMarker> received) {
		this.markers = received;
	}

	public Long2ObjectMap<String> getStructures() {
		return this.structures;
	}

	public List<AtlasMarker> getMarkers() {
		return this.markers;
	}

	public @Nullable AtlasTileTexture getTexture(final int chunkX, final int chunkZ) {
		int tile = this.tiles.get(ChunkPos.pack(chunkX, chunkZ));
		if (tile == WorldAtlas.NO_TILE) {
			return null;
		}

		int provider = WorldAtlas.getProvider(tile);
		if (provider >= this.palette.size()) {
			return null;
		}

		return AtlasTextures.get(Minecraft.getInstance().getResourceManager())
			.getTexture(this.palette.get(provider), WorldAtlas.getElevation(tile), chunkX, chunkZ);
	}
}
