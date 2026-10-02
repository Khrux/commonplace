package com.khrux.handbook.world.level.syndicate;

import com.khrux.handbook.compat.FieldGuideSyndication;
import com.khrux.handbook.network.protocol.FollowPassphrasePayload;
import com.khrux.handbook.network.protocol.PassphraseSettingsPayload;
import com.khrux.handbook.network.protocol.PassphraseSlotsPayload;
import com.khrux.handbook.network.protocol.SharePlainInkPayload;
import com.khrux.handbook.network.protocol.SheenPayload;
import com.khrux.handbook.world.entity.player.HandbookAttachmentTypes;
import com.khrux.handbook.world.entity.player.PassphraseSlot;
import com.khrux.handbook.world.level.atlas.AtlasTracker;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

public class Syndication {
	private static final int INTERVAL = 20;
	private static final Map<UUID, Syndication.Known> ACTIVE = new HashMap<>();
	private static final Set<String> PENDING_REFRESH = new HashSet<>();
	private static boolean fieldGuide;

	public static void bootstrap() {
		fieldGuide = FabricLoader.getInstance().isModLoaded("fieldguide");
		PayloadTypeRegistry.clientboundPlay().register(PassphraseSlotsPayload.TYPE, PassphraseSlotsPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SheenPayload.TYPE, SheenPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(FollowPassphrasePayload.TYPE, FollowPassphrasePayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PassphraseSettingsPayload.TYPE, PassphraseSettingsPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SharePlainInkPayload.TYPE, SharePlainInkPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(FollowPassphrasePayload.TYPE, (payload, context) -> follow(context.player(), payload.slot(), payload.hash()));
		ServerPlayNetworking.registerGlobalReceiver(
			PassphraseSettingsPayload.TYPE, (payload, context) -> changeSettings(context.player(), payload.slot(), payload.color(), payload.enderInk())
		);
		ServerPlayNetworking.registerGlobalReceiver(SharePlainInkPayload.TYPE, (payload, context) -> sharePlainInk(context.player(), payload.slot()));
		ServerTickEvents.END_SERVER_TICK.register(Syndication::tick);
		ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> join(listener.player));
		ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> disconnect(listener.player));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			ACTIVE.clear();
			PENDING_REFRESH.clear();
		});
	}

	static boolean isActive(final ServerPlayer player) {
		return ACTIVE.containsKey(player.getUUID());
	}

	public static boolean hasQuill(final ServerPlayer player) {
		return !player.getAttachedOrElse(HandbookAttachmentTypes.QUILL_SLOT, ItemStack.EMPTY).isEmpty();
	}

	static Syndicates getSyndicates(final MinecraftServer server) {
		return server.overworld().getAttachedOrCreate(HandbookAttachmentTypes.SYNDICATES);
	}

	static List<PassphraseSlot> getSlots(final ServerPlayer player) {
		return player.getAttachedOrCreate(HandbookAttachmentTypes.PASSPHRASE_SLOTS);
	}

	private static void tick(final MinecraftServer server) {
		PassphraseNotebooks.tick(server);
		if (!PENDING_REFRESH.isEmpty()) {
			Set<String> hashes = Set.copyOf(PENDING_REFRESH);
			PENDING_REFRESH.clear();
			hashes.forEach(hash -> refresh(server, hash));
		}

		if (server.getTickCount() % INTERVAL != 0) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			UUID uuid = player.getUUID();
			Syndication.Known known = ACTIVE.get(uuid);
			if (!hasQuill(player)) {
				ACTIVE.remove(uuid);
				continue;
			}

			if (known == null) {
				ACTIVE.put(uuid, new Syndication.Known(getRecipes(player), getEntries(player)));
				catchUp(player);
				continue;
			}

			Set<String> recipes = getRecipes(player);
			Set<String> entries = getEntries(player);
			List<String> newRecipes = recipes.stream().filter(recipe -> !known.recipes.contains(recipe)).toList();
			List<String> newEntries = entries.stream().filter(entry -> !known.entries.contains(entry)).toList();
			known.recipes = recipes;
			known.entries = entries;
			if (!newRecipes.isEmpty() || !newEntries.isEmpty()) {
				share(player, new SharedContent.Delta(newRecipes, newEntries, Map.of()));
			}
		}
	}

	public static void explore(final ServerPlayer player, final String dimension, final LongList positions) {
		if (ACTIVE.containsKey(player.getUUID()) && !positions.isEmpty()) {
			share(player, new SharedContent.Delta(List.of(), List.of(), Map.of(dimension, positions)));
		}
	}

	private static void share(final ServerPlayer player, final SharedContent.Delta content) {
		for (PassphraseSlot slot : getSlots(player)) {
			if (!slot.isEmpty() && slot.enderInk()) {
				contribute(player, slot.hash(), content);
			}
		}
	}

	private static void contribute(final ServerPlayer contributor, final String hash, final SharedContent.Delta content) {
		MinecraftServer server = contributor.level().getServer();
		Syndicate syndicate = getSyndicates(server).get(hash);
		if (syndicate == null) {
			return;
		}

		SharedContent.Delta added = syndicate.getContent().add(content);
		if (added.isEmpty()) {
			return;
		}

		for (UUID uuid : syndicate.getFollowers().keySet()) {
			ServerPlayer follower = server.getPlayerList().getPlayer(uuid);
			if (follower == null) {
				continue;
			}

			if (follower != contributor && ACTIVE.containsKey(uuid)) {
				deliver(follower, added);
			}

			List<PassphraseSlot> slots = getSlots(follower);
			for (int i = 0; i < slots.size(); i++) {
				if (slots.get(i).hash().equals(hash)) {
					sendSheen(follower, i, added);
				}
			}
		}
	}

	private static void deliver(final ServerPlayer player, final SharedContent.Delta content) {
		Syndication.Known known = ACTIVE.get(player.getUUID());
		if (known == null) {
			return;
		}

		List<RecipeHolder<?>> recipes = new ArrayList<>();
		for (String id : content.recipes()) {
			Identifier location = Identifier.tryParse(id);
			if (location == null) {
				continue;
			}

			ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, location);
			if (!player.getRecipeBook().contains(key)) {
				player.level().getServer().getRecipeManager().byKey(key).ifPresent(recipes::add);
			}
		}

		known.recipes.addAll(content.recipes());
		player.awardRecipes(recipes);
		if (fieldGuide && !content.entries().isEmpty()) {
			known.entries.addAll(content.entries());
			FieldGuideSyndication.unlock(player, content.entries());
		}

		content.chunks().forEach((dimension, positions) -> AtlasTracker.reveal(player, dimension, positions));
	}

	private static void catchUp(final ServerPlayer player) {
		Syndicates syndicates = getSyndicates(player.level().getServer());
		for (PassphraseSlot slot : getSlots(player)) {
			Syndicate syndicate = slot.isEmpty() ? null : syndicates.get(slot.hash());
			if (syndicate != null) {
				deliver(player, syndicate.getContent().getAll());
			}
		}

		PassphraseNotebooks.sendAll(player);
	}

	private static SharedContent.Delta getAllContent(final ServerPlayer player) {
		Map<String, LongCollection> chunks = new HashMap<>(player.getAttachedOrCreate(HandbookAttachmentTypes.EXPLORED).getDimensions());
		return new SharedContent.Delta(getRecipes(player), getEntries(player), chunks);
	}

	private static Set<String> getRecipes(final ServerPlayer player) {
		Set<String> recipes = new HashSet<>();
		for (ResourceKey<Recipe<?>> key : player.getRecipeBook().pack().known()) {
			recipes.add(key.identifier().toString());
		}

		return recipes;
	}

	private static Set<String> getEntries(final ServerPlayer player) {
		return fieldGuide ? FieldGuideSyndication.getEntries(player) : Set.of();
	}

	static void follow(final ServerPlayer player, final int index, final String hash) {
		if (index < 0 || index >= PassphraseSlot.SLOTS || !hash.isEmpty() && !PassphraseSlot.isValidHash(hash) || !hasQuill(player)) {
			return;
		}

		List<PassphraseSlot> slots = new ArrayList<>(getSlots(player));
		PassphraseSlot old = slots.get(index);
		if (old.hash().equals(hash) || !hash.isEmpty() && slots.stream().anyMatch(slot -> slot.hash().equals(hash))) {
			sendSlots(player);
			return;
		}

		Syndicates syndicates = getSyndicates(player.level().getServer());
		if (!old.isEmpty()) {
			Syndicate oldSyndicate = syndicates.get(old.hash());
			if (oldSyndicate != null) {
				oldSyndicate.getFollowers().remove(player.getUUID());
				PassphraseNotebooks.abandon(oldSyndicate, player.level().getServer().overworld().getGameTime());
				player.getAttachedOrCreate(HandbookAttachmentTypes.LEFT_CONTENT).add(oldSyndicate.getContent().intersect(getAllContent(player)));
			}

			PENDING_REFRESH.add(old.hash());
		}

		PassphraseSlot slot = old.withHash(hash);
		slots.set(index, slot);
		player.setAttached(HandbookAttachmentTypes.PASSPHRASE_SLOTS, List.copyOf(slots));
		if (!hash.isEmpty()) {
			Syndicate syndicate = syndicates.getOrCreate(hash);
			PassphraseNotebooks.rejoin(syndicate, player.level().getServer().overworld().getGameTime());
			syndicate.getFollowers().put(player.getUUID(), new Syndicate.Follower(player.getGameProfile().name(), slot.enderInk()));
			contribute(player, hash, getAllContent(player));
			deliver(player, syndicate.getContent().getAll());
			PENDING_REFRESH.add(hash);
		}

		sendSlots(player);
		sendAllSheen(player);
		if (isActive(player)) {
			PassphraseNotebooks.send(player, index);
		}
	}

	static void changeSettings(final ServerPlayer player, final int index, final int color, final boolean enderInk) {
		if (index < 0 || index >= PassphraseSlot.SLOTS || color < 0 || color > 15) {
			return;
		}

		List<PassphraseSlot> slots = new ArrayList<>(getSlots(player));
		PassphraseSlot slot = slots.get(index).withSettings(color, enderInk);
		slots.set(index, slot);
		player.setAttached(HandbookAttachmentTypes.PASSPHRASE_SLOTS, List.copyOf(slots));
		Syndicate syndicate = slot.isEmpty() ? null : getSyndicates(player.level().getServer()).get(slot.hash());
		if (syndicate != null && syndicate.getFollowers().containsKey(player.getUUID())) {
			syndicate.getFollowers().put(player.getUUID(), new Syndicate.Follower(player.getGameProfile().name(), enderInk));
			PENDING_REFRESH.add(slot.hash());
		}

		sendSlots(player);
	}

	static void sharePlainInk(final ServerPlayer player, final int index) {
		if (index < 0 || index >= PassphraseSlot.SLOTS || !hasQuill(player)) {
			return;
		}

		PassphraseSlot slot = getSlots(player).get(index);
		if (!slot.isEmpty()) {
			contribute(player, slot.hash(), getAllContent(player));
		}
	}

	private static void join(final ServerPlayer player) {
		Syndicates syndicates = getSyndicates(player.level().getServer());
		for (PassphraseSlot slot : getSlots(player)) {
			Syndicate syndicate = slot.isEmpty() ? null : syndicates.get(slot.hash());
			if (syndicate != null) {
				syndicate.getFollowers().put(player.getUUID(), new Syndicate.Follower(player.getGameProfile().name(), slot.enderInk()));
				PENDING_REFRESH.add(slot.hash());
			}
		}

		sendSlots(player);
		sendAllSheen(player);
	}

	private static void disconnect(final ServerPlayer player) {
		ACTIVE.remove(player.getUUID());
		for (PassphraseSlot slot : getSlots(player)) {
			if (!slot.isEmpty()) {
				PENDING_REFRESH.add(slot.hash());
			}
		}
	}

	private static void refresh(final MinecraftServer server, final String hash) {
		Syndicate syndicate = getSyndicates(server).get(hash);
		if (syndicate == null) {
			return;
		}

		for (UUID uuid : syndicate.getFollowers().keySet()) {
			ServerPlayer follower = server.getPlayerList().getPlayer(uuid);
			if (follower != null) {
				sendSlots(follower);
			}
		}
	}

	static void sendSlots(final ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, PassphraseSlotsPayload.TYPE)) {
			return;
		}

		MinecraftServer server = player.level().getServer();
		Syndicates syndicates = getSyndicates(server);
		List<PassphraseSlotsPayload.SlotView> views = new ArrayList<>();
		for (PassphraseSlot slot : getSlots(player)) {
			Syndicate syndicate = slot.isEmpty() ? null : syndicates.get(slot.hash());
			List<PassphraseSlotsPayload.FollowerView> followers = new ArrayList<>();
			if (syndicate != null) {
				syndicate.getFollowers().forEach((uuid, follower) -> followers.add(
					new PassphraseSlotsPayload.FollowerView(follower.name(), follower.enderInk(), server.getPlayerList().getPlayer(uuid) != null)
				));
				followers.sort(Comparator.comparing((PassphraseSlotsPayload.FollowerView follower) -> !follower.online()).thenComparing(PassphraseSlotsPayload.FollowerView::name));
			}

			views.add(new PassphraseSlotsPayload.SlotView(slot, List.copyOf(followers.subList(0, Math.min(followers.size(), PassphraseSlotsPayload.MAX_FOLLOWERS)))));
		}

		ServerPlayNetworking.send(player, new PassphraseSlotsPayload(views));
	}

	static void sendAllSheen(final ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, SheenPayload.TYPE)) {
			return;
		}

		ServerPlayNetworking.send(player, new SheenPayload(true, 0, List.of(), List.of(), "", new long[0]));
		Syndicates syndicates = getSyndicates(player.level().getServer());
		List<PassphraseSlot> slots = getSlots(player);
		for (int i = 0; i < slots.size(); i++) {
			Syndicate syndicate = slots.get(i).isEmpty() ? null : syndicates.get(slots.get(i).hash());
			if (syndicate != null) {
				sendSheen(player, i, syndicate.getContent().getAll());
			}
		}

		sendSheen(player, SheenPayload.LEFT_LAYER, player.getAttachedOrCreate(HandbookAttachmentTypes.LEFT_CONTENT).getAll());
	}

	private static void sendSheen(final ServerPlayer player, final int layer, final SharedContent.Delta content) {
		if (!ServerPlayNetworking.canSend(player, SheenPayload.TYPE) || content.isEmpty()) {
			return;
		}

		RecipeManager recipeManager = player.level().getServer().getRecipeManager();
		List<Integer> displays = new ArrayList<>();
		for (String id : content.recipes()) {
			Identifier location = Identifier.tryParse(id);
			if (location != null) {
				recipeManager.listDisplaysForRecipe(ResourceKey.create(Registries.RECIPE, location), display -> displays.add(display.id().index()));
			}
		}

		List<String> entries = List.copyOf(content.entries());
		for (int i = 0; i < Math.max(displays.size(), entries.size()); i += SheenPayload.MAX_BATCH) {
			List<Integer> displayBatch = displays.subList(Math.min(i, displays.size()), Math.min(i + SheenPayload.MAX_BATCH, displays.size()));
			List<String> entryBatch = entries.subList(Math.min(i, entries.size()), Math.min(i + SheenPayload.MAX_BATCH, entries.size()));
			ServerPlayNetworking.send(player, new SheenPayload(false, layer, List.copyOf(displayBatch), List.copyOf(entryBatch), "", new long[0]));
		}

		content.chunks().forEach((dimension, positions) -> {
			long[] all = positions.toLongArray();
			for (int i = 0; i < all.length; i += SheenPayload.MAX_BATCH) {
				ServerPlayNetworking.send(player, new SheenPayload(false, layer, List.of(), List.of(), dimension, Arrays.copyOfRange(all, i, Math.min(i + SheenPayload.MAX_BATCH, all.length))));
			}
		});
	}

	private static class Known {
		private Set<String> recipes;
		private Set<String> entries;

		private Known(final Set<String> recipes, final Set<String> entries) {
			this.recipes = recipes;
			this.entries = entries;
		}
	}
}
