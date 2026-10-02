package com.khrux.handbook.client.compat;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public class FieldGuideSketch {
	private static final int INK = 0xFF3F2A1C;
	private static final int PAPER = 0xFFEEDDB8;
	private static final float COLOR_KEPT = 0.5F;
	private static final float RIM_STRENGTH = 0.35F;
	private static final float HATCH_BELOW = 0.37F;
	private static final int HATCH_SPACING = 7;

	public static boolean isIcon(final String namespace, final String path) {
		return namespace.equals("fieldguide") && path.startsWith("generated_icon/") && !path.endsWith("_silhouette");
	}

	public static void sketch(final NativeImage image, final boolean page) {
		int width = image.getWidth();
		int height = image.getHeight();
		int[] colors = new int[width * height];
		float[] alpha = new float[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int color = image.getPixel(x, y);
				colors[x + y * width] = color;
				alpha[x + y * width] = ARGB.alpha(color) / 255.0F;
			}
		}

		float[] eroded = filter(alpha, width, height, page ? 3 : 7, false);
		float[] rim = new float[width * height];
		for (int i = 0; i < rim.length; i++) {
			rim[i] = Math.max(0.0F, alpha[i] - eroded[i]);
		}

		rim = blur(blur(rim, width, height, page ? 2 : 4), width, height, page ? 2 : 4);
		float[] outline = new float[width * height];
		if (page) {
			float[] dilated = filter(alpha, width, height, 1, true);
			float[] thinned = filter(alpha, width, height, 1, false);
			for (int i = 0; i < outline.length; i++) {
				outline[i] = Math.max(0.0F, dilated[i] - thinned[i]);
			}
		}

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width;
				int original = colors[i];
				float tone = 40.0F / 255.0F + luminance(original) * 0.85F;
				int color = ARGB.srgbLerp(COLOR_KEPT, ARGB.srgbLerp(tone, INK, PAPER), ARGB.opaque(original));
				color = ARGB.srgbLerp(rim[i] * RIM_STRENGTH, color, INK);
				if (page && tone < HATCH_BELOW && Math.floorMod(x - y, HATCH_SPACING) == 0) {
					color = ARGB.srgbLerp(0.45F, color, INK);
				}

				float ink = outline[i] * (0.65F + noise(x, y) * 0.35F);
				float shown = Math.max(alpha[i], ink);
				if (shown <= 0.0F) {
					image.setPixel(x, y, 0);
					continue;
				}

				color = alpha[i] <= 0.0F ? INK : ARGB.srgbLerp(ink, color, INK);
				image.setPixel(x, y, ARGB.color(Math.round(shown * 255.0F), color));
			}
		}
	}

	private static float luminance(final int color) {
		return (ARGB.red(color) * 0.299F + ARGB.green(color) * 0.587F + ARGB.blue(color) * 0.114F) / 255.0F;
	}

	private static float[] filter(final float[] values, final int width, final int height, final int radius, final boolean max) {
		float[] horizontal = new float[values.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float result = max ? 0.0F : 1.0F;
				for (int d = -radius; d <= radius; d++) {
					float value = x + d < 0 || x + d >= width ? 0.0F : values[x + d + y * width];
					result = max ? Math.max(result, value) : Math.min(result, value);
				}

				horizontal[x + y * width] = result;
			}
		}

		float[] result = new float[values.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float value = max ? 0.0F : 1.0F;
				for (int d = -radius; d <= radius; d++) {
					float sample = y + d < 0 || y + d >= height ? 0.0F : horizontal[x + (y + d) * width];
					value = max ? Math.max(value, sample) : Math.min(value, sample);
				}

				result[x + y * width] = value;
			}
		}

		return result;
	}

	private static float[] blur(final float[] values, final int width, final int height, final int radius) {
		float[] horizontal = new float[values.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float sum = 0.0F;
				for (int d = -radius; d <= radius; d++) {
					sum += values[Mth.clamp(x + d, 0, width - 1) + y * width];
				}

				horizontal[x + y * width] = sum / (radius * 2 + 1);
			}
		}

		float[] result = new float[values.length];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float sum = 0.0F;
				for (int d = -radius; d <= radius; d++) {
					sum += horizontal[x + Mth.clamp(y + d, 0, height - 1) * width];
				}

				result[x + y * width] = sum / (radius * 2 + 1);
			}
		}

		return result;
	}

	private static float noise(final int x, final int y) {
		int cellX = Math.floorDiv(x, 12);
		int cellY = Math.floorDiv(y, 12);
		float fx = Mth.smoothstep((x - cellX * 12) / 12.0F);
		float fy = Mth.smoothstep((y - cellY * 12) / 12.0F);
		float top = Mth.lerp(fx, hash(cellX, cellY), hash(cellX + 1, cellY));
		float bottom = Mth.lerp(fx, hash(cellX, cellY + 1), hash(cellX + 1, cellY + 1));
		return Mth.lerp(fy, top, bottom);
	}

	private static float hash(final int x, final int y) {
		int h = x * 374761393 + y * 668265263;
		h = (h ^ h >>> 13) * 1274126177;
		return ((h ^ h >>> 16) & 0xFFFF) / 65535.0F;
	}
}
