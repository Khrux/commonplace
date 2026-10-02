package com.khrux.handbook.mixin;

import com.khrux.handbook.world.entity.player.HandbookAttachmentTypes;
import com.khrux.handbook.world.inventory.HandbookSlot;
import com.khrux.handbook.world.inventory.ToolSlot;
import com.khrux.handbook.world.item.HandbookItems;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractCraftingMenu {
	private static final int FIRST_INVENTORY_SLOT = 9;
	private static final int LAST_INVENTORY_SLOT = 45;

	private InventoryMenuMixin() {
		super(null, 0, 2, 2);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void addHandbookSlot(final Inventory inventory, final boolean active, final Player owner, final CallbackInfo ci) {
		this.addSlot(new HandbookSlot(owner));
		this.addSlot(new ToolSlot(owner, HandbookAttachmentTypes.SPYGLASS_SLOT, Items.SPYGLASS, ToolSlot.SPYGLASS_X));
		this.addSlot(new ToolSlot(owner, HandbookAttachmentTypes.COMPASS_SLOT, Items.COMPASS, ToolSlot.COMPASS_X));
		this.addSlot(new ToolSlot(owner, HandbookAttachmentTypes.QUILL_SLOT, HandbookItems.ENDER_QUILL, ToolSlot.QUILL_X));
	}

	@Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
	private void moveToolIntoSlot(final Player player, final int slotIndex, final CallbackInfoReturnable<ItemStack> cir) {
		if (slotIndex < FIRST_INVENTORY_SLOT || slotIndex >= LAST_INVENTORY_SLOT || player.hasInfiniteMaterials()) {
			return;
		}

		Slot slot = this.slots.get(slotIndex);
		ItemStack itemStack = slot.getItem();
		for (int i = 0; i < this.slots.size(); i++) {
			if (this.slots.get(i) instanceof ToolSlot toolSlot && itemStack.is(toolSlot.getTool()) && !toolSlot.hasItem()) {
				ItemStack clicked = itemStack.copy();
				if (this.moveItemStackTo(itemStack, i, i + 1, false)) {
					if (itemStack.isEmpty()) {
						slot.setByPlayer(ItemStack.EMPTY, clicked);
					} else {
						slot.setChanged();
					}

					cir.setReturnValue(ItemStack.EMPTY);
				}

				return;
			}
		}
	}
}
