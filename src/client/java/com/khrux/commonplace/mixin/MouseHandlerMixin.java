package com.khrux.commonplace.mixin;

import com.khrux.commonplace.client.HandbookTabControls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(
		method = "onScroll",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getInventory()Lnet/minecraft/world/entity/player/Inventory;"),
		cancellable = true
	)
	private void turnHeldHandbookTabs(final long handle, final double xoffset, final double yoffset, final CallbackInfo ci) {
		if (HandbookTabControls.scroll(this.minecraft, yoffset)) {
			ci.cancel();
		}
	}
}
