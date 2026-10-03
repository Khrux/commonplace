package com.khrux.handbook.client.gui.components;

import com.khrux.handbook.HandbookConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;

public class EnderInk {
	private static final int VIOLET = 0xFF3B1D63;
	private static final int TEAL = 0xFF13695C;
	private static final int GLOW = 0xE6FFF8;
	private static final int GLOW_ALPHA = 70;
	private static final int[] STAR_COLORS = {0xBFF2E8, 0xD6C2FA, 0xFFFFFF, 0x9FE6D6};
	private static final int STARS = 150;
	private static final float[][] STAR_FIELD = new float[STARS][5];
	private static float forcedTeal = -1.0F;

	static {
		RandomSource random = RandomSource.create(0x3E7D3L);
		for (float[] star : STAR_FIELD) {
			star[0] = random.nextFloat();
			star[1] = random.nextFloat();
			star[2] = 0.4F + random.nextFloat() * 1.2F;
			star[3] = random.nextFloat() * Mth.TWO_PI;
			star[4] = random.nextInt(STAR_COLORS.length);
		}
	}

	public static void forceTeal(final float teal) {
		forcedTeal = teal;
	}

	public static boolean isForced() {
		return forcedTeal >= 0.0F;
	}

	public static void release() {
		forcedTeal = -1.0F;
	}

	private static float seconds() {
		return forcedTeal < 0.0F && HandbookConfig.get().enderPageMotion ? Util.getMillis() / 1000.0F : 0.0F;
	}

	public static float getTeal(final float x, final float y) {
		float phase = seconds() / 1.8F + (x + y * 0.6F) / 70.0F;
		return (Mth.sin(phase) + 1.0F) / 2.0F;
	}

	public static int color(final int x, final int y) {
		return ARGB.srgbLerp(forcedTeal < 0.0F ? getTeal(x, y) : forcedTeal, VIOLET, TEAL);
	}

	public static int color(final int x, final int y, final int alpha) {
		return ARGB.color(alpha, color(x, y));
	}

	public static void text(final GuiGraphicsExtractor graphics, final Font font, final Component text, final int x, final int y, final int alpha) {
		int glow = ARGB.color(GLOW_ALPHA * alpha / 255, GLOW);
		graphics.text(font, text, x - 1, y, glow, false);
		graphics.text(font, text, x + 1, y, glow, false);
		graphics.text(font, text, x, y - 1, glow, false);
		graphics.text(font, text, x, y + 1, glow, false);
		graphics.text(font, text, x, y, color(x, y, alpha), false);
	}

	public static void centeredText(final GuiGraphicsExtractor graphics, final Font font, final Component text, final int x, final int y) {
		text(graphics, font, text, x - font.width(text) / 2, y, 255);
	}

	public static void blot(final GuiGraphicsExtractor graphics, final int centerX, final int centerY, final float radius, final int color) {
		int reach = Mth.ceil(radius);
		for (int dy = -reach; dy <= reach; dy++) {
			int half = Mth.floor(Mth.sqrt(Math.max(0.0F, radius * radius - dy * dy)));
			if (half > 0 || Math.abs(dy) < radius) {
				graphics.fill(centerX - half, centerY + dy, centerX + half + 1, centerY + dy + 1, color);
			}
		}
	}

	public static void diamond(final GuiGraphicsExtractor graphics, final int centerX, final int centerY, final int radius, final int color) {
		for (int dy = -radius; dy <= radius; dy++) {
			int span = radius - Math.abs(dy);
			graphics.fill(centerX - span, centerY + dy, centerX + span + 1, centerY + dy + 1, color);
		}
	}

	public static void ring(final GuiGraphicsExtractor graphics, final int centerX, final int centerY, final float radius, final int color) {
		int reach = Mth.ceil(radius);
		for (int dy = -reach; dy <= reach; dy++) {
			for (int dx = -reach; dx <= reach; dx++) {
				float distance = Mth.sqrt(dx * dx + dy * dy);
				if (Math.abs(distance - radius) < 0.5F) {
					graphics.fill(centerX + dx, centerY + dy, centerX + dx + 1, centerY + dy + 1, color);
				}
			}
		}
	}

	public static void wavyLine(final GuiGraphicsExtractor graphics, final int x0, final int x1, final int y, final int alpha) {
		float drift = seconds() / 0.9F;
		for (int x = x0; x < x1; x++) {
			int offset = Math.round(Mth.sin(x / 3.0F + drift) * 0.7F);
			graphics.fill(x, y + offset, x + 1, y + offset + 1, color(x, y, alpha));
		}
	}

	public static void stars(final GuiGraphicsExtractor graphics, final int left, final int top, final int width, final int height, final int gutterX) {
		if (forcedTeal >= 0.0F) {
			return;
		}

		forEachStar(width, height, gutterX - left, (x, y, color, faint) -> {
			int px = left + x;
			int py = top + y;
			graphics.fill(px, py, px + 1, py + 1, color);
			if (faint != 0) {
				graphics.fill(px - 1, py, px, py + 1, faint);
				graphics.fill(px + 1, py, px + 2, py + 1, faint);
				graphics.fill(px, py - 1, px + 1, py, faint);
				graphics.fill(px, py + 1, px + 1, py + 2, faint);
			}
		});
	}

	public static void forEachStar(final int width, final int height, final int gutterX, final EnderInk.StarOutput output) {
		float time = seconds();
		for (float[] star : STAR_FIELD) {
			int x = (int)((star[0] * width + time * star[2] * 1.5F) % width);
			int y = (int)(((star[1] * height - time * star[2] * 2.5F) % height + height) % height);
			if (Math.abs(x - gutterX) < 4) {
				continue;
			}

			float twinkle = (Mth.sin(time * star[2] * 1.7F + star[3]) + 1.0F) / 2.0F;
			int alpha = (int)(40 + twinkle * twinkle * 215);
			int color = STAR_COLORS[(int)star[4]];
			output.accept(x, y, ARGB.color(alpha, color), star[2] > 1.2F && twinkle > 0.6F ? ARGB.color(alpha / 3, color) : 0);
		}
	}

	@FunctionalInterface
	public interface StarOutput {
		void accept(int x, int y, int color, int faint);
	}
}
