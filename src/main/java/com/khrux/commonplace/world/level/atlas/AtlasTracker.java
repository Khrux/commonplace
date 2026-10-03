package com.khrux.commonplace.world.level.atlas;

import com.khrux.commonplace.network.protocol.AtlasMarkersPayload;
import com.khrux.commonplace.network.protocol.AtlasTilesPayload;
import com.khrux.commonplace.network.protocol.PlaceAtlasMarkerPayload;
import com.khrux.commonplace.network.protocol.RemoveAtlasMarkerPayload;
import com.khrux.commonplace.world.entity.player.CommonplaceAttachmentTypes;
import com.khrux.commonplace.world.level.syndicate.Syndication;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

public class AtlasTracker {
	private static final int RADIUS = 6;
	private static final int INTERVAL = 10;
	private static final int MAX_MARKERS = 256;
	private static final Set<UUID> RESPAWNED = new HashSet<>();
	private static AtlasStructures structures = AtlasStructures.EMPTY;

	public static void bootstrap() {
		PayloadTypeRegistry.clientboundPlay().register(AtlasTilesPayload.TYPE, AtlasTilesPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(AtlasMarkersPayload.TYPE, AtlasMarkersPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PlaceAtlasMarkerPayload.TYPE, PlaceAtlasMarkerPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RemoveAtlasMarkerPayload.TYPE, RemoveAtlasMarkerPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(PlaceAtlasMarkerPayload.TYPE, (payload, context) -> placeMarker(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(RemoveAtlasMarkerPayload.TYPE, (payload, context) -> removeMarker(context.player(), payload.id()));
		ServerTickEvents.END_SERVER_TICK.register(AtlasTracker::tick);
		ServerLifecycleEvents.SERVER_STARTED.register(server -> structures = AtlasStructures.load(server.getResourceManager()));
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> structures = AtlasStructures.load(server.getResourceManager()));
		ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> sendAll(listener.player));
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> sendAll(player));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> RESPAWNED.add(newPlayer.getUUID()));
	}

	public static WorldAtlas getAtlas(final ServerLevel level) {
		return level.getAttachedOrCreate(CommonplaceAttachmentTypes.WORLD_ATLAS);
	}

	public static LongSet getExplored(final ServerPlayer player) {
		return player.getAttachedOrCreate(CommonplaceAttachmentTypes.EXPLORED).get(player.level().dimension().identifier().toString());
	}

	private static void tick(final MinecraftServer server) {
		if (!RESPAWNED.isEmpty()) {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (RESPAWNED.remove(player.getUUID())) {
					sendAll(player);
				}
			}

			RESPAWNED.clear();
		}

		if (server.getTickCount() % INTERVAL != 0) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isSpectator()) {
				survey(player);
			}
		}
	}

	private static void survey(final ServerPlayer player) {
		ServerLevel level = player.level();
		WorldAtlas atlas = getAtlas(level);
		LongSet explored = getExplored(player);
		ChunkPos center = player.chunkPosition();
		LongArrayList positions = new LongArrayList();
		IntArrayList tiles = new IntArrayList();
		LongArrayList discovered = new LongArrayList();
		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dz = -RADIUS; dz <= RADIUS; dz++) {
				long pos = ChunkPos.pack(center.x() + dx, center.z() + dz);
				LevelChunk chunk = level.getChunkSource().getChunkNow(center.x() + dx, center.z() + dz);
				if (chunk == null) {
					continue;
				}

				int tile = atlas.getTile(pos);
				if (tile == WorldAtlas.NO_TILE) {
					tile = AtlasSurveyor.survey(atlas, level, chunk);
				}

				boolean changed = false;
				if (!atlas.areStructuresChecked(pos)) {
					String marker = atlas.getStructureMarker(pos);
					if (AtlasSurveyor.surveyStructures(atlas, structures, level, chunk)) {
						atlas.markStructuresChecked(pos);
					}

					changed = atlas.getTile(pos) != tile || atlas.getStructureMarker(pos) != marker;
					tile = atlas.getTile(pos);
				}

				if (explored.contains(pos) && !changed) {
					continue;
				}

				if (explored.add(pos)) {
					discovered.add(pos);
				}

				positions.add(pos);
				tiles.add(tile);
			}
		}

		if (!positions.isEmpty()) {
			send(player, false, atlas, positions, tiles);
		}

		Syndication.explore(player, level.dimension().identifier().toString(), discovered);
	}

	public static void reveal(final ServerPlayer player, final String dimension, final LongCollection received) {
		LongSet explored = player.getAttachedOrCreate(CommonplaceAttachmentTypes.EXPLORED).get(dimension);
		boolean current = dimension.equals(player.level().dimension().identifier().toString());
		WorldAtlas atlas = getAtlas(player.level());
		LongArrayList positions = new LongArrayList();
		IntArrayList tiles = new IntArrayList();
		LongIterator iterator = received.iterator();
		while (iterator.hasNext()) {
			long pos = iterator.nextLong();
			if (!explored.add(pos) || !current) {
				continue;
			}

			int tile = atlas.getTile(pos);
			if (tile == WorldAtlas.NO_TILE) {
				continue;
			}

			positions.add(pos);
			tiles.add(tile);
			if (positions.size() == AtlasTilesPayload.MAX_BATCH) {
				send(player, false, atlas, positions, tiles);
				positions.clear();
				tiles.clear();
			}
		}

		if (!positions.isEmpty()) {
			send(player, false, atlas, positions, tiles);
		}
	}

	public static void sendAll(final ServerPlayer player) {
		WorldAtlas atlas = getAtlas(player.level());
		LongArrayList positions = new LongArrayList();
		IntArrayList tiles = new IntArrayList();
		boolean reset = true;
		LongIterator iterator = getExplored(player).iterator();
		while (iterator.hasNext()) {
			long pos = iterator.nextLong();
			int tile = atlas.getTile(pos);
			if (tile == WorldAtlas.NO_TILE) {
				continue;
			}

			positions.add(pos);
			tiles.add(tile);
			if (positions.size() == AtlasTilesPayload.MAX_BATCH) {
				send(player, reset, atlas, positions, tiles);
				reset = false;
				positions.clear();
				tiles.clear();
			}
		}

		if (reset || !positions.isEmpty()) {
			send(player, reset, atlas, positions, tiles);
		}

		sendMarkers(player);
	}

	private static void placeMarker(final ServerPlayer player, final PlaceAtlasMarkerPayload payload) {
		if (!Level.isInSpawnableBounds(new BlockPos(payload.x(), 0, payload.z()))) {
			return;
		}

		ServerLevel level = player.level();
		List<AtlasMarker> markers = new ArrayList<>(level.getAttachedOrCreate(CommonplaceAttachmentTypes.ATLAS_MARKERS));
		UUID owner = player.getUUID();
		if (markers.stream().filter(marker -> marker.owner().equals(owner)).count() >= MAX_MARKERS) {
			return;
		}

		String label = StringUtil.filterText(payload.label()).strip();
		markers.add(new AtlasMarker(UUID.randomUUID(), owner, payload.markerType(), payload.x(), payload.z(), label));
		level.setAttached(CommonplaceAttachmentTypes.ATLAS_MARKERS, List.copyOf(markers));
		sendMarkers(player);
	}

	private static void removeMarker(final ServerPlayer player, final UUID id) {
		ServerLevel level = player.level();
		List<AtlasMarker> markers = new ArrayList<>(level.getAttachedOrCreate(CommonplaceAttachmentTypes.ATLAS_MARKERS));
		if (markers.removeIf(marker -> marker.id().equals(id) && marker.owner().equals(player.getUUID()))) {
			level.setAttached(CommonplaceAttachmentTypes.ATLAS_MARKERS, List.copyOf(markers));
			sendMarkers(player);
		}
	}

	private static void sendMarkers(final ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, AtlasMarkersPayload.TYPE)) {
			UUID owner = player.getUUID();
			List<AtlasMarker> visible = player.level().getAttachedOrCreate(CommonplaceAttachmentTypes.ATLAS_MARKERS).stream().filter(marker -> marker.owner().equals(owner)).toList();
			ServerPlayNetworking.send(player, new AtlasMarkersPayload(visible));
		}
	}

	private static void send(final ServerPlayer player, final boolean reset, final WorldAtlas atlas, final LongArrayList positions, final IntArrayList tiles) {
		if (!ServerPlayNetworking.canSend(player, AtlasTilesPayload.TYPE)) {
			return;
		}

		LongArrayList structurePositions = new LongArrayList();
		List<String> structureMarkers = new ArrayList<>();
		for (int i = 0; i < positions.size(); i++) {
			String marker = atlas.getStructureMarker(positions.getLong(i));
			if (marker != null) {
				structurePositions.add(positions.getLong(i));
				structureMarkers.add(marker);
			}
		}

		ServerPlayNetworking.send(
			player, new AtlasTilesPayload(reset, atlas.getPalette(), positions.toLongArray(), tiles.toIntArray(), structurePositions.toLongArray(), structureMarkers)
		);
	}
}
