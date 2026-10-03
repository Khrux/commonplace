package com.khrux.commonplace.mixin;

import com.khrux.commonplace.client.renderer.HeldHandbookRenderer;
import com.khrux.commonplace.world.item.CommonplaceItems;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsAndItemsRendererMixin {
	@WrapOperation(
		method = "submitArmWithItem",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;has(Lnet/minecraft/core/component/DataComponentType;)Z")
	)
	private boolean holdHandbookLikeMap(final ItemStack itemStack, final DataComponentType<?> type, final Operation<Boolean> original) {
		return original.call(itemStack, type) || type == DataComponents.MAP_ID && itemStack.is(CommonplaceItems.HANDBOOK);
	}

	@Inject(method = "renderMap", at = @At("HEAD"), cancellable = true)
	private void renderHandbook(
		final PoseStack poseStack,
		final SubmitNodeCollector submitNodeCollector,
		final int lightCoords,
		final ItemStack itemStack,
		final boolean mainHand,
		final FirstPersonHandsAndItemsRenderState state,
		final CallbackInfo ci
	) {
		if (itemStack.is(CommonplaceItems.HANDBOOK)) {
			HeldHandbookRenderer.render(poseStack, submitNodeCollector, lightCoords);
			ci.cancel();
		}
	}
}
