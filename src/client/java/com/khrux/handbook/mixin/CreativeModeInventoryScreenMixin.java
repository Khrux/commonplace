package com.khrux.handbook.mixin;

import com.khrux.handbook.world.inventory.HandbookSlot;
import com.khrux.handbook.world.inventory.ToolSlot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {
	@Unique
	private final List<ItemStack> handbookSlots = new ArrayList<>();

	@Inject(method = "slotClicked", at = @At("HEAD"))
	private void rememberHandbookSlots(final @Nullable Slot slot, final int slotId, final int buttonNum, final ContainerInput containerInput, final CallbackInfo ci) {
		this.handbookSlots.clear();
		for (Slot target : Minecraft.getInstance().player.inventoryMenu.slots) {
			this.handbookSlots.add(target instanceof HandbookSlot || target instanceof ToolSlot ? target.getItem().copy() : ItemStack.EMPTY);
		}
	}

	@Inject(method = "slotClicked", at = @At("RETURN"))
	private void keepHandbookSlots(final @Nullable Slot slot, final int slotId, final int buttonNum, final ContainerInput containerInput, final CallbackInfo ci) {
		List<Slot> slots = Minecraft.getInstance().player.inventoryMenu.slots;
		for (int i = 0; i < slots.size() && i < this.handbookSlots.size(); i++) {
			Slot target = slots.get(i);
			if ((target instanceof HandbookSlot || target instanceof ToolSlot) && !ItemStack.matches(target.getItem(), this.handbookSlots.get(i))) {
				target.set(this.handbookSlots.get(i));
			}
		}
	}
}
