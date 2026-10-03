package com.khrux.commonplace.world.entity.player;

import com.khrux.commonplace.Commonplace;
import com.khrux.commonplace.world.item.CommonplaceItems;
import com.khrux.commonplace.world.level.atlas.AtlasMarker;
import com.khrux.commonplace.world.level.atlas.ExploredChunks;
import com.khrux.commonplace.world.level.atlas.WorldAtlas;
import com.khrux.commonplace.world.level.syndicate.SharedContent;
import com.khrux.commonplace.world.level.syndicate.Syndicates;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.item.ItemStack;

public class CommonplaceAttachmentTypes {
	public static final AttachmentType<ItemStack> HANDBOOK_SLOT = AttachmentRegistry.create(
		Commonplace.id("handbook_slot"),
		builder -> builder.persistent(ItemStack.OPTIONAL_CODEC).copyOnDeath().initializer(() -> new ItemStack(CommonplaceItems.HANDBOOK))
	);
	public static final AttachmentType<ItemStack> LAST_HANDBOOK = AttachmentRegistry.create(
		Commonplace.id("last_handbook"), builder -> builder.persistent(ItemStack.OPTIONAL_CODEC).copyOnDeath().initializer(() -> ItemStack.EMPTY)
	);
	public static final AttachmentType<ItemStack> SPYGLASS_SLOT = AttachmentRegistry.create(
		Commonplace.id("spyglass_slot"), builder -> builder.persistent(ItemStack.OPTIONAL_CODEC).copyOnDeath().initializer(() -> ItemStack.EMPTY)
	);
	public static final AttachmentType<ItemStack> COMPASS_SLOT = AttachmentRegistry.create(
		Commonplace.id("compass_slot"), builder -> builder.persistent(ItemStack.OPTIONAL_CODEC).copyOnDeath().initializer(() -> ItemStack.EMPTY)
	);
	public static final AttachmentType<ItemStack> QUILL_SLOT = AttachmentRegistry.create(
		Commonplace.id("quill_slot"), builder -> builder.persistent(ItemStack.OPTIONAL_CODEC).copyOnDeath().initializer(() -> ItemStack.EMPTY)
	);
	public static final AttachmentType<List<PassphraseSlot>> PASSPHRASE_SLOTS = AttachmentRegistry.create(
		Commonplace.id("passphrase_slots"),
		builder -> builder.persistent(PassphraseSlot.CODEC.sizeLimitedListOf(PassphraseSlot.SLOTS)).copyOnDeath().initializer(PassphraseSlot::createSlots)
	);
	public static final AttachmentType<SharedContent> LEFT_CONTENT = AttachmentRegistry.create(
		Commonplace.id("left_content"), builder -> builder.persistent(SharedContent.CODEC).copyOnDeath().initializer(SharedContent::new)
	);
	public static final AttachmentType<Syndicates> SYNDICATES = AttachmentRegistry.create(
		Commonplace.id("syndicates"), builder -> builder.persistent(Syndicates.CODEC).initializer(Syndicates::new)
	);
	public static final AttachmentType<List<NotePage>> NOTEBOOK = AttachmentRegistry.create(
		Commonplace.id("notebook"), builder -> builder.persistent(NotePage.CODEC.sizeLimitedListOf(NotePage.PAGES)).copyOnDeath()
	);
	public static final AttachmentType<WorldAtlas> WORLD_ATLAS = AttachmentRegistry.create(
		Commonplace.id("world_atlas"), builder -> builder.persistent(WorldAtlas.CODEC).initializer(WorldAtlas::new)
	);
	public static final AttachmentType<List<AtlasMarker>> ATLAS_MARKERS = AttachmentRegistry.create(
		Commonplace.id("atlas_markers"), builder -> builder.persistent(AtlasMarker.CODEC.listOf()).initializer(List::of)
	);
	public static final AttachmentType<ExploredChunks> EXPLORED = AttachmentRegistry.create(
		Commonplace.id("explored"), builder -> builder.persistent(ExploredChunks.CODEC).copyOnDeath().initializer(ExploredChunks::new)
	);

	public static void bootstrap() {
	}
}
