package com.khrux.commonplace.world.level.syndicate;

import com.khrux.commonplace.world.entity.player.NotePage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

public class Syndicate {
	public static final long FOLLOWED = -1L;
	public static final Codec<Syndicate> CODEC = RecordCodecBuilder.create(
		i -> i.group(
				Codec.unboundedMap(UUIDUtil.STRING_CODEC, Syndicate.Follower.CODEC).fieldOf("followers").forGetter(syndicate -> Map.copyOf(syndicate.followers)),
				SharedContent.CODEC.fieldOf("content").forGetter(syndicate -> syndicate.content),
				NotePage.CODEC.sizeLimitedListOf(NotePage.PAGES).optionalFieldOf("notes", List.of()).forGetter(syndicate -> List.copyOf(syndicate.notes)),
				Codec.LONG.optionalFieldOf("abandoned_at", FOLLOWED).forGetter(syndicate -> syndicate.abandonedAt)
			)
			.apply(i, Syndicate::new)
	);
	private final Map<UUID, Syndicate.Follower> followers = new LinkedHashMap<>();
	private final SharedContent content;
	private final List<NotePage> notes = new ArrayList<>();
	private long abandonedAt;

	public Syndicate() {
		this(Map.of(), new SharedContent(), List.of(), FOLLOWED);
	}

	private Syndicate(final Map<UUID, Syndicate.Follower> followers, final SharedContent content, final List<NotePage> notes, final long abandonedAt) {
		this.followers.putAll(followers);
		this.content = content;
		this.notes.addAll(notes);
		this.abandonedAt = abandonedAt;
	}

	public Map<UUID, Syndicate.Follower> getFollowers() {
		return this.followers;
	}

	public SharedContent getContent() {
		return this.content;
	}

	public List<NotePage> getNotes() {
		return this.notes;
	}

	public long getAbandonedAt() {
		return this.abandonedAt;
	}

	public void setAbandonedAt(final long abandonedAt) {
		this.abandonedAt = abandonedAt;
	}

	public record Follower(String name, boolean enderInk) {
		public static final Codec<Syndicate.Follower> CODEC = RecordCodecBuilder.create(
			i -> i.group(Codec.STRING.fieldOf("name").forGetter(Syndicate.Follower::name), Codec.BOOL.fieldOf("ender_ink").forGetter(Syndicate.Follower::enderInk))
				.apply(i, Syndicate.Follower::new)
		);
	}
}
