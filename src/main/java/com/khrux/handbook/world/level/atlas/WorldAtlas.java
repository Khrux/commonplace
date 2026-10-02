package com.khrux.handbook.world.level.atlas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.jspecify.annotations.Nullable;

public class WorldAtlas {
	public static final int NO_TILE = -1;
	public static final int NO_ELEVATION = 7;
	private static final int STRUCTURES_VERSION = 1;
	private static final Codec<long[]> LONG_ARRAY = Codec.LONG_STREAM.xmap(LongStream::toArray, Arrays::stream);
	private static final Codec<int[]> INT_ARRAY = Codec.INT_STREAM.xmap(IntStream::toArray, Arrays::stream);
	public static final Codec<WorldAtlas> CODEC = RecordCodecBuilder.<WorldAtlas.Stored>create(
			i -> i.group(
					Codec.STRING.listOf().fieldOf("palette").forGetter(WorldAtlas.Stored::palette),
					LONG_ARRAY.fieldOf("positions").forGetter(WorldAtlas.Stored::positions),
					INT_ARRAY.fieldOf("tiles").forGetter(WorldAtlas.Stored::tiles),
					LONG_ARRAY.optionalFieldOf("structure_positions", new long[0]).forGetter(WorldAtlas.Stored::structurePositions),
					Codec.STRING.listOf().optionalFieldOf("structure_markers", List.of()).forGetter(WorldAtlas.Stored::structureMarkers),
					LONG_ARRAY.optionalFieldOf("structures_checked", new long[0]).forGetter(WorldAtlas.Stored::structuresChecked),
					Codec.INT.optionalFieldOf("structures_version", 0).forGetter(WorldAtlas.Stored::structuresVersion)
				)
				.apply(i, WorldAtlas.Stored::new)
		)
		.xmap(WorldAtlas::new, WorldAtlas::store);
	private final List<String> palette = new ArrayList<>();
	private final Object2IntMap<String> paletteIndex = new Object2IntOpenHashMap<>();
	private final Long2IntMap tiles = new Long2IntOpenHashMap();
	private final Long2ObjectMap<String> structureMarkers = new Long2ObjectOpenHashMap<>();
	private final LongSet structuresChecked = new LongOpenHashSet();

	public WorldAtlas() {
		this.tiles.defaultReturnValue(NO_TILE);
	}

	private WorldAtlas(final WorldAtlas.Stored stored) {
		this();
		stored.palette().forEach(this::indexOf);
		for (int i = 0; i < stored.positions().length && i < stored.tiles().length; i++) {
			this.tiles.put(stored.positions()[i], stored.tiles()[i]);
		}

		for (int i = 0; i < stored.structurePositions().length && i < stored.structureMarkers().size(); i++) {
			this.structureMarkers.put(stored.structurePositions()[i], stored.structureMarkers().get(i));
		}

		if (stored.structuresVersion() == STRUCTURES_VERSION) {
			this.structuresChecked.addAll(LongArrayList.wrap(stored.structuresChecked()));
		}
	}

	private WorldAtlas.Stored store() {
		long[] positions = new long[this.tiles.size()];
		int[] packed = new int[this.tiles.size()];
		int i = 0;
		for (Long2IntMap.Entry entry : this.tiles.long2IntEntrySet()) {
			positions[i] = entry.getLongKey();
			packed[i] = entry.getIntValue();
			i++;
		}

		long[] structurePositions = new long[this.structureMarkers.size()];
		List<String> markers = new ArrayList<>();
		int j = 0;
		for (Long2ObjectMap.Entry<String> entry : this.structureMarkers.long2ObjectEntrySet()) {
			structurePositions[j++] = entry.getLongKey();
			markers.add(entry.getValue());
		}

		return new WorldAtlas.Stored(List.copyOf(this.palette), positions, packed, structurePositions, markers, this.structuresChecked.toLongArray(), STRUCTURES_VERSION);
	}

	public List<String> getPalette() {
		return this.palette;
	}

	public int getTile(final long pos) {
		return this.tiles.get(pos);
	}

	public int setTile(final long pos, final String provider, final int elevation) {
		int tile = pack(this.indexOf(provider), elevation);
		this.tiles.put(pos, tile);
		return tile;
	}

	public @Nullable String getStructureMarker(final long pos) {
		return this.structureMarkers.get(pos);
	}

	public void setStructureMarker(final long pos, final String marker) {
		this.structureMarkers.put(pos, marker);
	}

	public boolean areStructuresChecked(final long pos) {
		return this.structuresChecked.contains(pos);
	}

	public void markStructuresChecked(final long pos) {
		this.structuresChecked.add(pos);
	}

	private int indexOf(final String provider) {
		if (!this.paletteIndex.containsKey(provider)) {
			this.paletteIndex.put(provider, this.palette.size());
			this.palette.add(provider);
		}

		return this.paletteIndex.getInt(provider);
	}

	public static int pack(final int provider, final int elevation) {
		return provider << 3 | elevation;
	}

	public static int getProvider(final int tile) {
		return tile >>> 3;
	}

	public static int getElevation(final int tile) {
		return tile & 7;
	}

	private record Stored(
		List<String> palette,
		long[] positions,
		int[] tiles,
		long[] structurePositions,
		List<String> structureMarkers,
		long[] structuresChecked,
		int structuresVersion
	) {
	}
}
