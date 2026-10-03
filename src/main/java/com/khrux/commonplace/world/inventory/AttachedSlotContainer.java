package com.khrux.commonplace.world.inventory;

import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class AttachedSlotContainer implements Container {
	private final Player player;
	private final AttachmentType<ItemStack> type;

	public AttachedSlotContainer(final Player player, final AttachmentType<ItemStack> type) {
		this.player = player;
		this.type = type;
	}

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return this.getItem(0).isEmpty();
	}

	@Override
	public ItemStack getItem(final int slot) {
		return this.player.getAttachedOrCreate(this.type);
	}

	@Override
	public ItemStack removeItem(final int slot, final int count) {
		ItemStack removed = this.getItem(slot).split(count);
		this.setChanged();
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(final int slot) {
		ItemStack itemStack = this.getItem(slot);
		this.setItem(slot, ItemStack.EMPTY);
		return itemStack;
	}

	@Override
	public void setItem(final int slot, final ItemStack itemStack) {
		this.player.setAttached(this.type, itemStack);
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public void setChanged() {
	}

	@Override
	public boolean stillValid(final Player player) {
		return true;
	}

	@Override
	public void clearContent() {
		this.setItem(0, ItemStack.EMPTY);
	}
}
