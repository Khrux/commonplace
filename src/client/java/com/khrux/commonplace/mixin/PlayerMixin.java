package com.khrux.commonplace.mixin;

import com.khrux.commonplace.client.SpyglassZoom;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {
	@Inject(method = "isScoping", at = @At("HEAD"), cancellable = true)
	private void scopeWithSpyglassSlot(final CallbackInfoReturnable<Boolean> cir) {
		if (SpyglassZoom.isZooming() && (Object) this == Minecraft.getInstance().player) {
			cir.setReturnValue(true);
		}
	}
}
