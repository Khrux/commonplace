package com.khrux.commonplace.world.level.syndicate;

import com.mojang.serialization.Codec;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public class Syndicates {
	public static final Codec<Syndicates> CODEC = Codec.unboundedMap(Codec.STRING, Syndicate.CODEC).xmap(Syndicates::new, syndicates -> syndicates.byHash);
	private final Map<String, Syndicate> byHash = new HashMap<>();

	public Syndicates() {
	}

	private Syndicates(final Map<String, Syndicate> stored) {
		this.byHash.putAll(stored);
	}

	public @Nullable Syndicate get(final String hash) {
		return this.byHash.get(hash);
	}

	public Collection<Syndicate> getAll() {
		return this.byHash.values();
	}

	public Syndicate getOrCreate(final String hash) {
		return this.byHash.computeIfAbsent(hash, key -> new Syndicate());
	}
}
