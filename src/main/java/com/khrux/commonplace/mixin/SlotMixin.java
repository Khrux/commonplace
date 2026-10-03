package com.khrux.commonplace.mixin;

import com.khrux.commonplace.world.inventory.AttachedSlotContainer;
import com.khrux.commonplace.world.item.CommonplaceItems;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMixin {
	@Shadow
	@Final
	public Container container;

	@Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
	private void keepHandbookInInventory(final ItemStack itemStack, final CallbackInfoReturnable<Boolean> cir) {
		if (itemStack.is(CommonplaceItems.HANDBOOK) && !(this.container instanceof Inventory) && !(this.container instanceof AttachedSlotContainer)
			&& !(this.container instanceof TransientCraftingContainer)) {
			cir.setReturnValue(false);
		}
	}
}
