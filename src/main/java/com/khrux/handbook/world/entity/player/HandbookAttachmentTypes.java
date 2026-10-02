package com.khrux.handbook.world.entity.player;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.world.item.HandbookItems;
import com.khrux.handbook.world.level.atlas.AtlasMarker;
import com.khrux.handbook.world.level.atlas.ExploredChunks;
import com.khrux.handbook.world.level.atlas.WorldAtlas;
import com.khrux.handbook.world.level.syndicate.SharedContent;
import com.khrux.handbook.world.level.syndicate.Syndicates;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.item.ItemStack;

public class HandbookAttachmentTypes {
	public static final AttachmentType<ItemStack> HANDBOOK_SLOT = AttachmentRegistry.create(
		Handbook.id("handbook_slot"),
		builder -> builder.persistent(ItemStack.OPTIONAL_CODEC).copyOnDeath().initializer(() -> new ItemStack(HandbookItems.HANDBOOK))
	);
	public static final AttachmentType<ItemStack> LAST_HANDBOOK = AttachmentRegistry.create(
		Handbook.id("last_handbook"), builder -> builder.persistent(ItemStack.OPTIONAL_CODEC).copyOnDeath().initializer(() -> ItemStack.EMPTY)
	);
	public static final AttachmentType<ItemStack> SPYGLASS_SLOT = AttachmentRegistry.create(
		Handbook.id("spyglass_slot"), builder -> builder.persistent(ItemStack.OPTIONAL_CODEC).copyOnDeath().initializer(() -> ItemStack.EMPTY)
	);
	public static final AttachmentType<ItemStack> COMPASS_SLOT = AttachmentRegistry.create(
		Handbook.id("compass_slot"), builder -> builder.persistent(ItemStack.OPTIONAL_CODEC).copyOnDeath().initializer(() -> ItemStack.EMPTY)
	);
	public static final AttachmentType<ItemStack> QUILL_SLOT = AttachmentRegistry.create(
		Handbook.id("quill_slot"), builder -> builder.persistent(ItemStack.OPTIONAL_CODEC).copyOnDeath().initializer(() -> ItemStack.EMPTY)
	);
	public static final AttachmentType<List<PassphraseSlot>> PASSPHRASE_SLOTS = AttachmentRegistry.create(
		Handbook.id("passphrase_slots"),
		builder -> builder.persistent(PassphraseSlot.CODEC.sizeLimitedListOf(PassphraseSlot.SLOTS)).copyOnDeath().initializer(PassphraseSlot::createSlots)
	);
	public static final AttachmentType<SharedContent> LEFT_CONTENT = AttachmentRegistry.create(
		Handbook.id("left_content"), builder -> builder.persistent(SharedContent.CODEC).copyOnDeath().initializer(SharedContent::new)
	);
	public static final AttachmentType<Syndicates> SYNDICATES = AttachmentRegistry.create(
		Handbook.id("syndicates"), builder -> builder.persistent(Syndicates.CODEC).initializer(Syndicates::new)
	);
	public static final AttachmentType<List<NotePage>> NOTEBOOK = AttachmentRegistry.create(
		Handbook.id("notebook"), builder -> builder.persistent(NotePage.CODEC.sizeLimitedListOf(NotePage.PAGES)).copyOnDeath()
	);
	public static final AttachmentType<WorldAtlas> WORLD_ATLAS = AttachmentRegistry.create(
		Handbook.id("world_atlas"), builder -> builder.persistent(WorldAtlas.CODEC).initializer(WorldAtlas::new)
	);
	public static final AttachmentType<List<AtlasMarker>> ATLAS_MARKERS = AttachmentRegistry.create(
		Handbook.id("atlas_markers"), builder -> builder.persistent(AtlasMarker.CODEC.listOf()).initializer(List::of)
	);
	public static final AttachmentType<ExploredChunks> EXPLORED = AttachmentRegistry.create(
		Handbook.id("explored"), builder -> builder.persistent(ExploredChunks.CODEC).copyOnDeath().initializer(ExploredChunks::new)
	);

	public static void bootstrap() {
	}
}
