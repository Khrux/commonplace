package com.khrux.handbook.client.gui.hud;

import com.khrux.handbook.HandbookConfig;
import com.khrux.handbook.world.entity.player.HandbookAttachmentTypes;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CompassCoordinates {
	private static final int MARGIN = 2;
	private static final int EFFECT_ROW = 26;

	public static void extractRenderState(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		HandbookConfig.CoordinatesPosition position = HandbookConfig.get().coordinatesPosition;
		if (player == null || position == HandbookConfig.CoordinatesPosition.HIDDEN || !player.getAttachedOrElse(HandbookAttachmentTypes.COMPASS_SLOT, ItemStack.EMPTY).is(Items.COMPASS)) {
			return;
		}

		BlockPos pos = player.blockPosition();
		Font font = minecraft.font;
		Component text = Component.translatable("handbook.compass.coordinates", pos.getX(), pos.getY(), pos.getZ());
		int x = switch (position) {
			case TOP_LEFT, BOTTOM_LEFT -> MARGIN;
			default -> graphics.guiWidth() - font.width(text) - MARGIN;
		};
		int y = switch (position) {
			case BOTTOM_LEFT, BOTTOM_RIGHT -> graphics.guiHeight() - font.lineHeight - MARGIN;
			case TOP_LEFT -> MARGIN;
			default -> effectsY(player);
		};
		if (minecraft.isDemo() && position == HandbookConfig.CoordinatesPosition.TOP_RIGHT) {
			y += 15;
		}

		graphics.text(font, text, x, y, -1, true);
	}

	private static int effectsY(final LocalPlayer player) {
		boolean beneficial = false;
		boolean harmful = false;
		for (MobEffectInstance instance : player.getActiveEffects()) {
			if (instance.showIcon()) {
				if (instance.getEffect().value().isBeneficial()) {
					beneficial = true;
				} else {
					harmful = true;
				}
			}
		}

		return harmful ? 1 + EFFECT_ROW * 2 : beneficial ? 1 + EFFECT_ROW : MARGIN;
	}
}
