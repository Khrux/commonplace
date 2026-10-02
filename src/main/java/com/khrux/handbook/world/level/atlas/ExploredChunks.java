package com.khrux.handbook.world.level.atlas;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.LongStream;

public class ExploredChunks {
	private static final Codec<long[]> LONG_ARRAY = Codec.LONG_STREAM.xmap(LongStream::toArray, Arrays::stream);
	public static final Codec<ExploredChunks> CODEC = Codec.unboundedMap(Codec.STRING, LONG_ARRAY).xmap(ExploredChunks::new, ExploredChunks::store);
	private final Map<String, LongSet> dimensions = new HashMap<>();

	public ExploredChunks() {
	}

	private ExploredChunks(final Map<String, long[]> stored) {
		stored.forEach((dimension, positions) -> this.dimensions.put(dimension, new LongOpenHashSet(positions)));
	}

	private Map<String, long[]> store() {
		Map<String, long[]> stored = new HashMap<>();
		this.dimensions.forEach((dimension, positions) -> stored.put(dimension, positions.toLongArray()));
		return stored;
	}

	public Map<String, LongSet> getDimensions() {
		return this.dimensions;
	}

	public LongSet get(final String dimension) {
		return this.dimensions.computeIfAbsent(dimension, key -> new LongOpenHashSet());
	}
}
