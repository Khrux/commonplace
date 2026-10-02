package com.khrux.handbook.mixin;

import com.khrux.handbook.client.SpyglassZoom;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {
	@Inject(method = "getFieldOfViewModifier", at = @At("HEAD"), cancellable = true)
	private void zoomWithSpyglassSlot(final boolean firstPerson, final float effectScale, final CallbackInfoReturnable<Float> cir) {
		if (firstPerson && SpyglassZoom.isZooming() && ((AbstractClientPlayer) (Object) this).isScoping()) {
			cir.setReturnValue(0.1F);
		}
	}
}
