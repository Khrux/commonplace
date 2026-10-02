package com.khrux.handbook.world.inventory;

import com.khrux.handbook.world.entity.player.HandbookAttachmentTypes;
import com.khrux.handbook.world.item.HandbookItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class HandbookSlot extends Slot {
	public static final int X = 105;
	public static final int Y = 62;
	private final Player owner;

	public HandbookSlot(final Player owner) {
		super(new AttachedSlotContainer(owner, HandbookAttachmentTypes.HANDBOOK_SLOT), 0, X, Y);
		this.owner = owner;
	}

	@Override
	public boolean mayPlace(final ItemStack itemStack) {
		return itemStack.is(HandbookItems.HANDBOOK);
	}

	@Override
	public boolean isActive() {
		return !this.owner.hasInfiniteMaterials();
	}
}
