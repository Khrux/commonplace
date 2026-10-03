package com.khrux.commonplace.world.level.syndicate;

import com.google.common.hash.Hashing;
import com.khrux.commonplace.CommonplaceConfig;
import com.khrux.commonplace.compat.FieldGuideSyndication;
import com.khrux.commonplace.network.protocol.NoteEditPayload;
import com.khrux.commonplace.world.entity.player.CommonplaceAttachmentTypes;
import com.khrux.commonplace.world.entity.player.NotePage;
import com.khrux.commonplace.world.entity.player.PassphraseSlot;
import com.khrux.commonplace.world.item.CommonplaceItems;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SyndicationGameTests {
	private static final int SETTLE = 45;

	private static String newHash() {
		return Hashing.sha256().hashString(UUID.randomUUID().toString(), StandardCharsets.UTF_8).toString();
	}

	private static ServerPlayer player(final GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setAttached(CommonplaceAttachmentTypes.QUILL_SLOT, new ItemStack(CommonplaceItems.ENDER_QUILL));
		return player;
	}

	private static ResourceKey<Recipe<?>> recipe(final String name) {
		return ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace(name));
	}

	private static void forget(final ServerPlayer player, final ResourceKey<Recipe<?>> recipe) {
		player.getRecipeBook().remove(recipe);
	}

	private static void learn(final ServerPlayer player, final ResourceKey<Recipe<?>> recipe) {
		player.awardRecipes(List.of(player.level().getServer().getRecipeManager().byKey(recipe).orElseThrow()));
	}

	private static boolean knows(final ServerPlayer player, final ResourceKey<Recipe<?>> recipe) {
		return player.getRecipeBook().contains(recipe);
	}

	private static PassphraseSlot slot(final ServerPlayer player, final int index) {
		return player.getAttachedOrCreate(CommonplaceAttachmentTypes.PASSPHRASE_SLOTS).get(index);
	}

	private static Syndicate syndicate(final GameTestHelper helper, final String hash) {
		return helper.getLevel().getServer().overworld().getAttachedOrCreate(CommonplaceAttachmentTypes.SYNDICATES).get(hash);
	}

	private static void remove(final GameTestHelper helper, final ServerPlayer... players) {
		for (ServerPlayer player : players) {
			helper.getLevel().getServer().getPlayerList().remove(player);
		}
	}

	@GameTest(maxTicks = 300)
	public void followSharesBothWays(final GameTestHelper helper) {
		ServerPlayer a = player(helper);
		ServerPlayer b = player(helper);
		ResourceKey<Recipe<?>> fromA = recipe("lodestone");
		ResourceKey<Recipe<?>> fromB = recipe("jukebox");
		String hash = newHash();
		for (ServerPlayer player : List.of(a, b)) {
			forget(player, fromA);
			forget(player, fromB);
		}

		learn(a, fromA);
		learn(b, fromB);
		helper.startSequence()
			.thenIdle(25)
			.thenExecute(() -> {
				Syndication.follow(a, 0, hash);
				Syndication.follow(b, 0, hash);
			})
			.thenWaitUntil(() -> {
				helper.assertTrue(knows(b, fromA), "B should receive A's recipe on following");
				helper.assertTrue(knows(a, fromB), "A should receive B's recipe when B follows");
				helper.assertTrue(syndicate(helper, hash).getFollowers().size() == 2, "both should be followers");
			})
			.thenExecute(() -> remove(helper, a, b))
			.thenSucceed();
	}

	@GameTest(maxTicks = 300)
	public void newDiscoveryIsShared(final GameTestHelper helper) {
		ServerPlayer a = player(helper);
		ServerPlayer b = player(helper);
		ResourceKey<Recipe<?>> later = recipe("beacon");
		String hash = newHash();
		forget(a, later);
		forget(b, later);
		helper.startSequence()
			.thenIdle(25)
			.thenExecute(() -> {
				Syndication.follow(a, 0, hash);
				Syndication.follow(b, 0, hash);
			})
			.thenIdle(5)
			.thenExecute(() -> learn(a, later))
			.thenWaitUntil(() -> helper.assertTrue(knows(b, later), "B should receive A's new discovery"))
			.thenExecute(() -> remove(helper, a, b))
			.thenSucceed();
	}

	@GameTest(maxTicks = 400)
	public void plainInkWithholdsUntilShared(final GameTestHelper helper) {
		ServerPlayer a = player(helper);
		ServerPlayer b = player(helper);
		ResourceKey<Recipe<?>> secret = recipe("conduit");
		String hash = newHash();
		forget(a, secret);
		forget(b, secret);
		helper.startSequence()
			.thenIdle(25)
			.thenExecute(() -> {
				Syndication.follow(a, 0, hash);
				Syndication.follow(b, 0, hash);
				Syndication.changeSettings(b, 0, 3, false);
			})
			.thenExecute(() -> {
				helper.assertFalse(slot(b, 0).enderInk(), "B's slot should be on plain ink");
				helper.assertFalse(syndicate(helper, hash).getFollowers().get(b.getUUID()).enderInk(), "B should be listed on plain ink");
				learn(b, secret);
			})
			.thenIdle(SETTLE)
			.thenExecute(() -> {
				helper.assertFalse(knows(a, secret), "plain ink should withhold B's discovery");
				Syndication.sharePlainInk(b, 0);
			})
			.thenWaitUntil(() -> helper.assertTrue(knows(a, secret), "Share my plain ink should deliver it"))
			.thenExecute(() -> remove(helper, a, b))
			.thenSucceed();
	}

	@GameTest(maxTicks = 400)
	public void receivedContentIsNotReshared(final GameTestHelper helper) {
		ServerPlayer a = player(helper);
		ServerPlayer b = player(helper);
		ServerPlayer c = player(helper);
		ResourceKey<Recipe<?>> fromB = recipe("respawn_anchor");
		String first = newHash();
		String second = newHash();
		for (ServerPlayer player : List.of(a, b, c)) {
			forget(player, fromB);
		}

		helper.startSequence()
			.thenIdle(25)
			.thenExecute(() -> {
				Syndication.follow(a, 0, first);
				Syndication.follow(b, 0, first);
				Syndication.follow(a, 1, second);
				Syndication.follow(c, 0, second);
			})
			.thenIdle(5)
			.thenExecute(() -> learn(b, fromB))
			.thenWaitUntil(() -> helper.assertTrue(knows(a, fromB), "A should receive B's discovery"))
			.thenIdle(SETTLE)
			.thenExecute(() -> helper.assertFalse(knows(c, fromB), "A must not pass B's content on to its other passphrase"))
			.thenExecute(() -> remove(helper, a, b, c))
			.thenSucceed();
	}

	@GameTest(maxTicks = 500)
	public void quillOutPausesAndCatchesUp(final GameTestHelper helper) {
		ServerPlayer a = player(helper);
		ServerPlayer b = player(helper);
		ResourceKey<Recipe<?>> fromB = recipe("lectern");
		ResourceKey<Recipe<?>> fromA = recipe("loom");
		String hash = newHash();
		for (ServerPlayer player : List.of(a, b)) {
			forget(player, fromA);
			forget(player, fromB);
		}

		helper.startSequence()
			.thenIdle(25)
			.thenExecute(() -> {
				Syndication.follow(a, 0, hash);
				Syndication.follow(b, 0, hash);
				a.removeAttached(CommonplaceAttachmentTypes.QUILL_SLOT);
			})
			.thenIdle(25)
			.thenExecute(() -> {
				learn(b, fromB);
				learn(a, fromA);
			})
			.thenIdle(SETTLE)
			.thenExecute(() -> {
				helper.assertFalse(knows(a, fromB), "A should receive nothing while its quill is out");
				helper.assertFalse(knows(b, fromA), "A should share nothing while its quill is out");
				a.setAttached(CommonplaceAttachmentTypes.QUILL_SLOT, new ItemStack(CommonplaceItems.ENDER_QUILL));
			})
			.thenWaitUntil(() -> helper.assertTrue(knows(a, fromB), "A should catch up when the quill goes back"))
			.thenIdle(SETTLE)
			.thenExecute(() -> helper.assertFalse(knows(b, fromA), "discoveries made with the quill out stay withheld"))
			.thenExecute(() -> remove(helper, a, b))
			.thenSucceed();
	}

	@GameTest(maxTicks = 400)
	public void leavingKeepsContentAndStopsSync(final GameTestHelper helper) {
		ServerPlayer a = player(helper);
		ServerPlayer b = player(helper);
		ResourceKey<Recipe<?>> kept = recipe("target");
		ResourceKey<Recipe<?>> after = recipe("recovery_compass");
		String hash = newHash();
		for (ServerPlayer player : List.of(a, b)) {
			forget(player, kept);
			forget(player, after);
		}

		learn(b, kept);
		helper.startSequence()
			.thenIdle(25)
			.thenExecute(() -> {
				Syndication.follow(a, 0, hash);
				Syndication.follow(b, 0, hash);
			})
			.thenWaitUntil(() -> helper.assertTrue(knows(a, kept), "A should receive B's recipe"))
			.thenExecute(() -> {
				Syndication.follow(a, 0, "");
				helper.assertTrue(slot(a, 0).isEmpty(), "A's slot should be empty after leaving");
				helper.assertFalse(syndicate(helper, hash).getFollowers().containsKey(a.getUUID()), "A should no longer follow");
				helper.assertTrue(
					a.getAttachedOrCreate(CommonplaceAttachmentTypes.LEFT_CONTENT).getAll().recipes().contains(kept.identifier().toString()),
					"what A received should be remembered for grey ink"
				);
				helper.assertTrue(knows(a, kept), "A keeps what it received");
				learn(b, after);
			})
			.thenIdle(SETTLE)
			.thenExecute(() -> helper.assertFalse(knows(a, after), "nothing syncs after leaving"))
			.thenExecute(() -> remove(helper, a, b))
			.thenSucceed();
	}

	@GameTest(maxTicks = 300)
	public void mapChunksAreShared(final GameTestHelper helper) {
		ServerPlayer a = player(helper);
		ServerPlayer b = player(helper);
		String dimension = helper.getLevel().dimension().identifier().toString();
		long before = ChunkPos.pack(30000, 30000);
		long later = ChunkPos.pack(30001, 30000);
		String hash = newHash();
		a.getAttachedOrCreate(CommonplaceAttachmentTypes.EXPLORED).get(dimension).add(before);
		helper.startSequence()
			.thenIdle(25)
			.thenExecute(() -> {
				Syndication.follow(a, 0, hash);
				Syndication.follow(b, 0, hash);
			})
			.thenExecute(() -> {
				helper.assertTrue(b.getAttachedOrCreate(CommonplaceAttachmentTypes.EXPLORED).get(dimension).contains(before), "B should receive A's mapped chunk");
				LongList positions = new LongArrayList();
				positions.add(later);
				a.getAttachedOrCreate(CommonplaceAttachmentTypes.EXPLORED).get(dimension).add(later);
				Syndication.explore(a, dimension, positions);
				helper.assertTrue(b.getAttachedOrCreate(CommonplaceAttachmentTypes.EXPLORED).get(dimension).contains(later), "B should receive A's newly mapped chunk");
			})
			.thenExecute(() -> remove(helper, a, b))
			.thenSucceed();
	}

	@GameTest(maxTicks = 100)
	public void invalidFollowsAreRejected(final GameTestHelper helper) {
		ServerPlayer a = player(helper);
		ServerPlayer noQuill = helper.makeMockServerPlayerInLevel();
		String hash = newHash();
		Syndication.follow(a, 0, "not a hash");
		helper.assertTrue(slot(a, 0).isEmpty(), "an invalid hash should be ignored");
		Syndication.follow(a, 7, hash);
		Syndication.follow(a, 0, hash);
		Syndication.follow(a, 1, hash);
		helper.assertTrue(slot(a, 0).hash().equals(hash), "a valid hash should be followed");
		helper.assertTrue(slot(a, 1).isEmpty(), "one passphrase can't be in two slots");
		Syndication.follow(noQuill, 0, hash);
		helper.assertTrue(slot(noQuill, 0).isEmpty(), "following needs a quill");
		remove(helper, a, noQuill);
		helper.succeed();
	}

	@GameTest(maxTicks = 300)
	public void fieldGuideEntriesAreShared(final GameTestHelper helper) {
		if (!FabricLoader.getInstance().isModLoaded("fieldguide")) {
			helper.succeed();
			return;
		}

		ServerPlayer a = player(helper);
		ServerPlayer b = player(helper);
		String entry = "entity:minecraft/axolotl";
		String hash = newHash();
		helper.startSequence()
			.thenIdle(25)
			.thenExecute(() -> {
				FieldGuideSyndication.unlock(a, List.of(entry));
				helper.assertTrue(FieldGuideSyndication.getEntries(a).contains(entry), "A should have unlocked the entry (Field Guide progress for mock players)");
				helper.assertFalse(FieldGuideSyndication.getEntries(b).contains(entry), "B should start without the entry");
				Syndication.follow(a, 0, hash);
				Syndication.follow(b, 0, hash);
			})
			.thenWaitUntil(() -> helper.assertTrue(FieldGuideSyndication.getEntries(b).contains(entry), "B should receive A's Field Guide entry"))
			.thenExecute(() -> remove(helper, a, b))
			.thenSucceed();
	}

	@GameTest(maxTicks = 200)
	public void passphraseNotebookEditsAreShared(final GameTestHelper helper) {
		ServerPlayer a = player(helper);
		ServerPlayer b = player(helper);
		ServerPlayer outsider = player(helper);
		String hash = newHash();
		int[] stroke = {NoteEditPayload.pack(3, 4, 9), NoteEditPayload.pack(5, 6, 2)};
		helper.startSequence()
			.thenIdle(25)
			.thenExecute(() -> {
				Syndication.follow(a, 0, hash);
				Syndication.follow(b, 0, hash);
				PassphraseNotebooks.edit(a, 0, 1, stroke, Optional.of("meet at the tower"));
				PassphraseNotebooks.edit(b, 0, 1, new int[]{NoteEditPayload.pack(5, 6, 0)}, Optional.empty());
				PassphraseNotebooks.edit(outsider, 0, 1, new int[]{NoteEditPayload.pack(10, 10, 4)}, Optional.empty());
				PassphraseNotebooks.edit(a, 0, CommonplaceConfig.getNoteOptions().passphrasePages(), stroke, Optional.empty());
				NotePage page = syndicate(helper, hash).getNotes().get(1);
				helper.assertTrue(page.getPixel(3, 4) == 9, "A's stroke should be stored");
				helper.assertTrue(page.getPixel(5, 6) == 0, "B's later erase should win");
				helper.assertTrue(page.getPixel(10, 10) == 0, "a non-follower can't draw");
				helper.assertTrue(page.text().equals("meet at the tower"), "text should be stored");
				helper.assertTrue(syndicate(helper, hash).getNotes().size() <= CommonplaceConfig.getNoteOptions().passphrasePages(), "pages past the limit are refused");
			})
			.thenExecute(() -> remove(helper, a, b, outsider))
			.thenSucceed();
	}

	@GameTest
	public void blackInkOnlyTurnsColoursBlack(final GameTestHelper helper) {
		int[] pixels = PassphraseNotebooks.allowedPixels(new int[]{NoteEditPayload.pack(1, 1, 4), NoteEditPayload.pack(2, 2, 0)}, CommonplaceConfig.NoteColors.BLACK);
		helper.assertTrue(NoteEditPayload.getColor(pixels[0]) == 15 && NoteEditPayload.getColor(pixels[1]) == 0, "black-only should ink black and still erase");
		helper.assertTrue(PassphraseNotebooks.allowedPixels(pixels, CommonplaceConfig.NoteColors.TEXT).length == 0, "text-only should refuse drawing");
		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void abandonedNotebooksFadeGradually(final GameTestHelper helper) {
		ServerPlayer a = player(helper);
		String hash = newHash();
		int[] fill = new int[NotePage.CANVAS_WIDTH * 8];
		for (int i = 0; i < fill.length; i++) {
			fill[i] = NoteEditPayload.pack(i % NotePage.CANVAS_WIDTH, i / NotePage.CANVAS_WIDTH, 15);
		}

		helper.startSequence()
			.thenIdle(25)
			.thenExecute(() -> {
				Syndication.follow(a, 0, hash);
				PassphraseNotebooks.edit(a, 0, 0, fill, Optional.of("a secret note"));
				Syndication.follow(a, 0, "");
				Syndicate syndicate = syndicate(helper, hash);
				long start = syndicate.getAbandonedAt();
				helper.assertTrue(start != Syndicate.FOLLOWED, "an empty passphrase should start fading");
				long fadeTicks = CommonplaceConfig.getNoteOptions().fadeDays() * 24000L;
				PassphraseNotebooks.fade(syndicate, start + fadeTicks / 2);
				int left = 0;
				for (int pixel : fill) {
					if (syndicate.getNotes().get(0).getPixel(NoteEditPayload.getX(pixel), NoteEditPayload.getY(pixel)) != 0) {
						left++;
					}
				}

				helper.assertTrue(left > fill.length / 4 && left < fill.length * 3 / 4, "half way, about half the ink should remain, got " + left);
				PassphraseNotebooks.fade(syndicate, start + fadeTicks);
				helper.assertTrue(syndicate.getNotes().isEmpty(), "after the fade time the notebook should be gone");
			})
			.thenExecute(() -> remove(helper, a))
			.thenSucceed();
	}

	@GameTest
	public void shiftClickMovesToolsIntoSlots(final GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.getAbilities().instabuild = false;
		player.getInventory().setItem(20, new ItemStack(Items.SPYGLASS));
		player.getInventory().setItem(21, new ItemStack(CommonplaceItems.ENDER_QUILL));
		player.getInventory().setItem(22, new ItemStack(Items.SPYGLASS));
		player.setAttached(CommonplaceAttachmentTypes.HANDBOOK_SLOT, ItemStack.EMPTY);
		player.getInventory().setItem(23, new ItemStack(CommonplaceItems.HANDBOOK));
		player.inventoryMenu.quickMoveStack(player, 20);
		player.inventoryMenu.quickMoveStack(player, 21);
		player.inventoryMenu.quickMoveStack(player, 22);
		player.inventoryMenu.quickMoveStack(player, 23);
		helper.assertTrue(player.getAttachedOrElse(CommonplaceAttachmentTypes.HANDBOOK_SLOT, ItemStack.EMPTY).is(CommonplaceItems.HANDBOOK), "the Handbook should go into its slot");
		helper.assertTrue(player.getInventory().getItem(23).isEmpty(), "the Handbook should leave the inventory");
		helper.assertTrue(player.getAttachedOrElse(CommonplaceAttachmentTypes.SPYGLASS_SLOT, ItemStack.EMPTY).is(Items.SPYGLASS), "the spyglass should go into its slot");
		helper.assertTrue(player.getAttachedOrElse(CommonplaceAttachmentTypes.QUILL_SLOT, ItemStack.EMPTY).is(CommonplaceItems.ENDER_QUILL), "the quill should go into its slot");
		helper.assertTrue(player.getInventory().getItem(20).isEmpty() && player.getInventory().getItem(21).isEmpty(), "the moved items should leave the inventory");
		helper.assertTrue(player.getInventory().contains(itemStack -> itemStack.is(Items.SPYGLASS)), "a second spyglass stays in the inventory");
		remove(helper, player);
		helper.succeed();
	}

	@GameTest(maxTicks = 100)
	public void swappingSlotsReordersPassphrases(final GameTestHelper helper) {
		ServerPlayer player = player(helper);
		String first = newHash();
		String second = newHash();
		Syndication.follow(player, 0, first);
		Syndication.follow(player, 3, second);
		Syndication.changeSettings(player, 3, 5, false);
		Syndication.swap(player, 0, 3);
		helper.assertTrue(slot(player, 0).hash().equals(second) && slot(player, 0).color() == 5 && !slot(player, 0).enderInk(), "slot 0 should hold the second passphrase with its colour and ink");
		helper.assertTrue(slot(player, 3).hash().equals(first), "slot 3 should hold the first passphrase");
		Syndication.swap(player, 3, 4);
		helper.assertTrue(slot(player, 3).isEmpty() && slot(player, 4).hash().equals(first), "swapping with an empty slot moves the passphrase");
		helper.assertTrue(syndicate(helper, first).getFollowers().containsKey(player.getUUID()) && syndicate(helper, second).getFollowers().containsKey(player.getUUID()), "reordering keeps following both");
		remove(helper, player);
		helper.succeed();
	}

	@GameTest(maxTicks = 100)
	public void deathKeepsTheBookAndDropsTools(final GameTestHelper helper) {
		ServerPlayer player = player(helper);
		String hash = newHash();
		Syndication.follow(player, 2, hash);
		player.setAttached(CommonplaceAttachmentTypes.SPYGLASS_SLOT, new ItemStack(Items.SPYGLASS));
		player.setAttached(CommonplaceAttachmentTypes.HANDBOOK_SLOT, new ItemStack(CommonplaceItems.HANDBOOK));
		player.setAttached(CommonplaceAttachmentTypes.NOTEBOOK, List.of(NotePage.EMPTY.withText("kept")));
		Vec3 inside = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
		player.setPos(inside.x, inside.y, inside.z);
		player.die(player.damageSources().genericKill());
		ServerPlayer respawned = helper.getLevel().getServer().getPlayerList().respawn(player, false, net.minecraft.world.entity.Entity.RemovalReason.KILLED);
		helper.assertTrue(respawned.getAttachedOrElse(CommonplaceAttachmentTypes.QUILL_SLOT, ItemStack.EMPTY).isEmpty(), "the quill should drop on death");
		helper.assertTrue(respawned.getAttachedOrElse(CommonplaceAttachmentTypes.SPYGLASS_SLOT, ItemStack.EMPTY).isEmpty(), "the spyglass should drop on death");
		helper.assertTrue(respawned.getAttachedOrElse(CommonplaceAttachmentTypes.HANDBOOK_SLOT, ItemStack.EMPTY).is(CommonplaceItems.HANDBOOK), "the Handbook stays in its slot");
		helper.assertTrue(slot(respawned, 2).hash().equals(hash), "passphrases survive death");
		helper.assertTrue(respawned.getAttachedOrElse(CommonplaceAttachmentTypes.NOTEBOOK, List.of()).getFirst().text().equals("kept"), "the personal notebook survives death");
		helper.assertTrue(
			!helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(inside, inside).inflate(4.0), entity -> entity.getItem().is(CommonplaceItems.ENDER_QUILL)).isEmpty(),
			"the quill should be on the ground"
		);
		remove(helper, respawned);
		helper.succeed();
	}

	@GameTest(maxTicks = 600)
	public void performanceAtScale(final GameTestHelper helper) {
		int followers = 100;
		List<ServerPlayer> players = new java.util.ArrayList<>();
		for (int i = 0; i < followers; i++) {
			players.add(player(helper));
		}

		String hash = newHash();
		String crowded = newHash();
		String dimension = helper.getLevel().dimension().identifier().toString();
		helper.startSequence()
			.thenIdle(25)
			.thenExecute(() -> {
				long start = System.nanoTime();
				for (ServerPlayer player : players) {
					Syndication.follow(player, 0, hash);
				}

				report("follow, 100 players one after another", System.nanoTime() - start, followers);
				Syndicate big = helper.getLevel().getServer().overworld().getAttachedOrCreate(CommonplaceAttachmentTypes.SYNDICATES).getOrCreate(crowded);
				LongList chunks = new LongArrayList();
				for (int x = 0; x < 300; x++) {
					for (int z = 0; z < 300; z++) {
						chunks.add(ChunkPos.pack(x + 50000, z));
					}
				}

				big.getContent().add(new SharedContent.Delta(List.of(), List.of(), java.util.Map.of(dimension, chunks)));
				long join = System.nanoTime();
				Syndication.follow(players.get(0), 1, crowded);
				report("join a passphrase holding 90,000 map chunks", System.nanoTime() - join, 1);
				ResourceKey<Recipe<?>> recipe = recipe("lodestone");
				players.forEach(player -> forget(player, recipe));
				learn(players.get(1), recipe);
				List<net.minecraft.world.item.crafting.RecipeHolder<?>> everything = List.copyOf(helper.getLevel().getServer().getRecipeManager().getRecipes());
				players.forEach(player -> player.awardRecipes(everything));
				long poll = System.nanoTime();
				try {
					java.lang.reflect.Method recipes = Syndication.class.getDeclaredMethod("getRecipes", ServerPlayer.class);
					recipes.setAccessible(true);
					for (ServerPlayer player : players) {
						recipes.invoke(null, player);
					}
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}

				report("discovery poll reading " + everything.size() + " known recipes for each of 100 players (runs once a second)", System.nanoTime() - poll, 1);
				LongList explored = new LongArrayList();
				for (int i = 0; i < 169; i++) {
					explored.add(ChunkPos.pack(90000 + i % 13, i / 13));
				}

				long explore = System.nanoTime();
				Syndication.explore(players.get(2), dimension, explored);
				report("share 169 newly explored chunks (one survey) to 99 followers", System.nanoTime() - explore, 1);
				int[] stroke = new int[64];
				for (int i = 0; i < stroke.length; i++) {
					stroke[i] = NoteEditPayload.pack(i, 10, 5);
				}

				long note = System.nanoTime();
				for (int i = 0; i < 100; i++) {
					PassphraseNotebooks.edit(players.get(i), 0, 0, stroke, Optional.empty());
				}

				report("notebook stroke of 64 pixels, from each of 100 players", System.nanoTime() - note, 100);
			})
			.thenExecute(() -> players.forEach(player -> remove(helper, player)))
			.thenSucceed();
	}

	private static void report(final String what, final long nanos, final int count) {
		System.out.println("HANDBOOK PERF " + what + ": total " + String.format("%.2f", nanos / 1.0E6) + " ms, each " + String.format("%.3f", nanos / 1.0E6 / count) + " ms");
	}
}