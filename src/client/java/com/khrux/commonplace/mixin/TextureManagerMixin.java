package com.khrux.commonplace.mixin;

import com.khrux.commonplace.CommonplaceConfig;
import com.khrux.commonplace.client.ClientHandbook;
import com.khrux.commonplace.client.ClientSheen;
import com.khrux.commonplace.client.compat.FieldGuideSketch;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureManager.class)
public class TextureManagerMixin {
	@Inject(method = "register(Lnet/minecraft/resources/Identifier;Lnet/minecraft/client/renderer/texture/AbstractTexture;)V", at = @At("HEAD"))
	private void sketchFieldGuideIcons(final Identifier location, final AbstractTexture texture, final CallbackInfo ci) {
		if (CommonplaceConfig.get().fieldGuideSketch && texture instanceof DynamicTexture dynamic && FieldGuideSketch.isIcon(location.getNamespace(), location.getPath())) {
			String path = location.getPath();
			FieldGuideSketch.sketch(dynamic.getPixels(), ClientHandbook.get().sheen().getIconSheen(path.substring(path.indexOf('/') + 1)));
			dynamic.upload();
		}
	}
}
