package com.khrux.commonplace.world.inventory;

import com.khrux.commonplace.world.entity.player.CommonplaceAttachmentTypes;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;

public class ToolSlot extends Slot {
	public static final int SPYGLASS_X = HandbookSlot.X + 18;
	public static final int COMPASS_X = HandbookSlot.X + 36;
	public static final int QUILL_X = HandbookSlot.X + 54;
	private static boolean revealed;
	private final Player owner;
	private final Item tool;

	public ToolSlot(final Player owner, final AttachmentType<ItemStack> type, final Item tool, final int x) {
		super(new AttachedSlotContainer(owner, type), 0, x, HandbookSlot.Y);
		this.owner = owner;
		this.tool = tool;
	}

	public Item getTool() {
		return this.tool;
	}

	@Override
	public boolean mayPlace(final ItemStack itemStack) {
		return itemStack.is(this.tool);
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public boolean isActive() {
		return !this.owner.hasInfiniteMaterials() && (revealed || !this.owner.level().isClientSide());
	}

	public static void setRevealed(final boolean shown) {
		revealed = shown;
	}

	public static void dropOnDeath(final LivingEntity entity) {
		if (!(entity instanceof ServerPlayer player) || player.level().getGameRules().get(GameRules.KEEP_INVENTORY)) {
			return;
		}

		for (AttachmentType<ItemStack> type : List.of(CommonplaceAttachmentTypes.SPYGLASS_SLOT, CommonplaceAttachmentTypes.COMPASS_SLOT, CommonplaceAttachmentTypes.QUILL_SLOT)) {
			ItemStack itemStack = player.removeAttached(type);
			if (itemStack != null && !itemStack.isEmpty()) {
				ItemEntity drop = player.createItemStackToDrop(itemStack, true, false);
				if (drop != null) {
					player.level().addFreshEntity(drop);
				}
			}
		}
	}
}
