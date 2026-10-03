package com.khrux.commonplace.world.level.syndicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;

public class SharedContent {
	private static final Codec<long[]> LONG_ARRAY = Codec.LONG_STREAM.xmap(LongStream::toArray, Arrays::stream);
	public static final Codec<SharedContent> CODEC = RecordCodecBuilder.<SharedContent.Stored>create(
			i -> i.group(
					Codec.STRING.listOf().fieldOf("recipes").forGetter(SharedContent.Stored::recipes),
					Codec.STRING.listOf().fieldOf("entries").forGetter(SharedContent.Stored::entries),
					Codec.unboundedMap(Codec.STRING, LONG_ARRAY).fieldOf("chunks").forGetter(SharedContent.Stored::chunks)
				)
				.apply(i, SharedContent.Stored::new)
		)
		.xmap(SharedContent::new, SharedContent::store);
	private final Set<String> recipes = new HashSet<>();
	private final Set<String> entries = new HashSet<>();
	private final Map<String, LongSet> chunks = new HashMap<>();

	public SharedContent() {
	}

	private SharedContent(final SharedContent.Stored stored) {
		this.recipes.addAll(stored.recipes());
		this.entries.addAll(stored.entries());
		stored.chunks().forEach((dimension, positions) -> this.chunks.put(dimension, new LongOpenHashSet(positions)));
	}

	private SharedContent.Stored store() {
		Map<String, long[]> storedChunks = new HashMap<>();
		this.chunks.forEach((dimension, positions) -> storedChunks.put(dimension, positions.toLongArray()));
		return new SharedContent.Stored(List.copyOf(this.recipes), List.copyOf(this.entries), storedChunks);
	}

	public SharedContent.Delta getAll() {
		Map<String, LongList> allChunks = new HashMap<>();
		this.chunks.forEach((dimension, positions) -> allChunks.put(dimension, new LongArrayList(positions)));
		return new SharedContent.Delta(List.copyOf(this.recipes), List.copyOf(this.entries), allChunks);
	}

	public SharedContent.Delta add(final SharedContent.Delta delta) {
		List<String> newRecipes = new ArrayList<>();
		for (String recipe : delta.recipes()) {
			if (this.recipes.add(recipe)) {
				newRecipes.add(recipe);
			}
		}

		List<String> newEntries = new ArrayList<>();
		for (String entry : delta.entries()) {
			if (this.entries.add(entry)) {
				newEntries.add(entry);
			}
		}

		Map<String, LongList> newChunks = new HashMap<>();
		delta.chunks().forEach((dimension, positions) -> {
			LongSet known = this.chunks.computeIfAbsent(dimension, key -> new LongOpenHashSet());
			LongList added = new LongArrayList();
			LongIterator iterator = positions.iterator();
			while (iterator.hasNext()) {
				long pos = iterator.nextLong();
				if (known.add(pos)) {
					added.add(pos);
				}
			}

			if (!added.isEmpty()) {
				newChunks.put(dimension, added);
			}
		});
		return new SharedContent.Delta(newRecipes, newEntries, newChunks);
	}

	public SharedContent.Delta intersect(final SharedContent.Delta delta) {
		List<String> sharedRecipes = delta.recipes().stream().filter(this.recipes::contains).toList();
		List<String> sharedEntries = delta.entries().stream().filter(this.entries::contains).toList();
		Map<String, LongList> sharedChunks = new HashMap<>();
		delta.chunks().forEach((dimension, positions) -> {
			LongSet known = this.chunks.get(dimension);
			if (known == null) {
				return;
			}

			LongList shared = new LongArrayList();
			LongIterator iterator = positions.iterator();
			while (iterator.hasNext()) {
				long pos = iterator.nextLong();
				if (known.contains(pos)) {
					shared.add(pos);
				}
			}

			if (!shared.isEmpty()) {
				sharedChunks.put(dimension, shared);
			}
		});
		return new SharedContent.Delta(sharedRecipes, sharedEntries, sharedChunks);
	}

	public record Delta(Collection<String> recipes, Collection<String> entries, Map<String, ? extends LongCollection> chunks) {
		public boolean isEmpty() {
			return this.recipes.isEmpty() && this.entries.isEmpty() && this.chunks.isEmpty();
		}
	}

	private record Stored(List<String> recipes, List<String> entries, Map<String, long[]> chunks) {
	}
}
