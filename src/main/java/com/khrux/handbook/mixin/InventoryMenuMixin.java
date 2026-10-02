package com.khrux.handbook.mixin;

import com.khrux.handbook.world.entity.player.HandbookAttachmentTypes;
import com.khrux.handbook.world.inventory.HandbookSlot;
import com.khrux.handbook.world.inventory.ToolSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractCraftingMenu {
	private InventoryMenuMixin() {
		super(null, 0, 2, 2);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void addHandbookSlot(final Inventory inventory, final boolean active, final Player owner, final CallbackInfo ci) {
		this.addSlot(new HandbookSlot(owner));
		this.addSlot(new ToolSlot(owner, HandbookAttachmentTypes.SPYGLASS_SLOT, Items.SPYGLASS, ToolSlot.SPYGLASS_X));
		this.addSlot(new ToolSlot(owner, HandbookAttachmentTypes.COMPASS_SLOT, Items.COMPASS, ToolSlot.COMPASS_X));
	}
}
