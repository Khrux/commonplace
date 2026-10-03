package com.khrux.commonplace.client.compat;

import com.evandev.fieldguide.client.gui.util.IconCacheManager;
import com.khrux.commonplace.client.ClientSheen;
import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Arrays;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public class FieldGuideSketch {
	private static final int INK = 0xFF422A1A;
	private static final int PAPER = 0xFFEEDDB8;
	private static final float SHEEN_STRENGTH = 0.7F;
	private static final float DIMMED_ALPHA = 0.32F;
	private static final float TONE_STRENGTH = 0.66F;
	private static final float OUTLINE = 0.9F;
	private static final float HOLE_OUTLINE = 0.3F;
	private static final float EDGE_BELOW = 0.75F;
	private static final int STROKE = 2;

	public static void refresh() {
		IconCacheManager.clearCache();
	}

	public static boolean isIcon(final String namespace, final String path) {
		return namespace.equals("fieldguide") && path.startsWith("generated_icon/") && !path.endsWith("_silhouette");
	}

	public static void sketch(final NativeImage image, final int sheen) {
		int ink = sheen != ClientSheen.NONE && sheen != ClientSheen.DIMMED ? ARGB.srgbLerp(SHEEN_STRENGTH, INK, sheen) : INK;
		float shown = sheen == ClientSheen.DIMMED ? DIMMED_ALPHA : 1.0F;
		int width = image.getWidth();
		int height = image.getHeight();
		float[] alpha = new float[width * height];
		float[] value = new float[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int color = image.getPixel(x, y);
				int i = x + y * width;
				alpha[i] = ARGB.alpha(color) / 255.0F;
				float luminance = (ARGB.red(color) * 0.299F + ARGB.green(color) * 0.587F + ARGB.blue(color) * 0.114F) / 255.0F;
				float brightest = Math.max(ARGB.red(color), Math.max(ARGB.green(color), ARGB.blue(color))) / 255.0F;
				value[i] = luminance * 0.55F + brightest * 0.45F;
			}
		}

		float[] lightness = levels(value, alpha);
		boolean[] outside = outside(alpha, width, height);
		int spacing = Math.max(6, width / 32);
		int[] bottom = new int[width];
		Arrays.fill(bottom, -1);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (alpha[x + y * width] > 0.5F) {
					bottom[x] = y;
				}
			}
		}

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = x + y * width;
				if (alpha[i] <= 0.0F) {
					image.setPixel(x, y, 0);
					continue;
				}

				float dark = Mth.clamp((1.0F - lightness[i] - 0.15F) * 1.25F, 0.0F, 1.0F);
				float tone = Math.min(1.0F, dark * TONE_STRENGTH + hash(x, y, 3) * 0.07F);
				float strength = Math.max(hatching(x, y, dark, spacing), edges(alpha, lightness, outside, width, height, x, y));
				int color = ARGB.srgbLerp(strength, ARGB.srgbLerp(tone, PAPER, INK), ink);
				image.setPixel(x, y, ARGB.color(Math.round(alpha[i] * shown * 255.0F), color));
			}
		}

		groundStrokes(image, bottom, spacing, ink, shown);
	}

	private static float[] levels(final float[] value, final float[] alpha) {
		float[] opaque = new float[value.length];
		int count = 0;
		for (int i = 0; i < value.length; i++) {
			if (alpha[i] > 0.5F) {
				opaque[count++] = value[i];
			}
		}

		Arrays.sort(opaque, 0, count);
		float low = count == 0 ? 0.0F : opaque[count / 20];
		float high = count == 0 ? 1.0F : opaque[count * 19 / 20];
		float span = Math.max(0.15F, high - low);
		float[] lightness = new float[value.length];
		for (int i = 0; i < value.length; i++) {
			lightness[i] = Mth.clamp(0.08F + (value[i] - low) / span * 0.82F, 0.0F, 1.0F);
		}

		return lightness;
	}

	private static boolean[] outside(final float[] alpha, final int width, final int height) {
		boolean[] outside = new boolean[width * height];
		IntArrayList stack = new IntArrayList();
		for (int x = 0; x < width; x++) {
			stack.add(x);
			stack.add(x + (height - 1) * width);
		}

		for (int y = 0; y < height; y++) {
			stack.add(y * width);
			stack.add(width - 1 + y * width);
		}

		while (!stack.isEmpty()) {
			int i = stack.popInt();
			if (outside[i] || alpha[i] > 0.5F) {
				continue;
			}

			outside[i] = true;
			int x = i % width;
			int y = i / width;
			if (x > 0) {
				stack.add(i - 1);
			}

			if (x < width - 1) {
				stack.add(i + 1);
			}

			if (y > 0) {
				stack.add(i - width);
			}

			if (y < height - 1) {
				stack.add(i + width);
			}
		}

		return outside;
	}

	private static float hatching(final int x, final int y, final float dark, final int spacing) {
		float strength = 0.0F;
		int wobble = (int)(smooth((x - y) / 9.0F, 1) * 2.0F);
		if (dark > 0.28F && Math.floorMod(x + y + wobble, spacing) < STROKE && hash(Math.floorDiv(x + y, 5), Math.floorDiv(x - y, spacing), 7) > 0.15F) {
			strength = 0.3F + (dark - 0.28F) * 0.8F;
		}

		int crossWobble = (int)(smooth((x + y) / 9.0F, 2) * 2.0F);
		if (dark > 0.55F && Math.floorMod(x - y + crossWobble, spacing) < STROKE && hash(Math.floorDiv(x - y, 5), Math.floorDiv(x + y, spacing), 9) > 0.2F) {
			strength = Math.max(strength, 0.35F + (dark - 0.55F) * 0.8F);
		}

		if (dark > 0.8F && Math.floorMod(y + (int)(smooth(x / 7.0F, 3) * 2.0F), spacing) < STROKE) {
			strength = Math.max(strength, 0.45F);
		}

		return Math.min(1.0F, strength);
	}

	private static float edges(final float[] alpha, final float[] lightness, final boolean[] outside, final int width, final int height, final int x, final int y) {
		float here = alpha[x + y * width];
		float silhouette = 0.0F;
		float gradientX = 0.0F;
		float gradientY = 0.0F;
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int reach = 1; reach <= 2; reach++) {
					int i = Mth.clamp(x + dx * reach, 0, width - 1) + Mth.clamp(y + dy * reach, 0, height - 1) * width;
					if (here - alpha[i] > 0.5F) {
						silhouette = Math.max(silhouette, outside[i] ? 1.0F : HOLE_OUTLINE);
					}
				}

				float neighbour = lightness[Mth.clamp(x + dx, 0, width - 1) + Mth.clamp(y + dy, 0, height - 1) * width];
				gradientX += neighbour * dx * (dy == 0 ? 2 : 1);
				gradientY += neighbour * dy * (dx == 0 ? 2 : 1);
			}
		}

		if (silhouette >= 1.0F) {
			return OUTLINE * (0.75F + hash(x, y, 5) * 0.25F);
		}

		float edge = Mth.sqrt(gradientX * gradientX + gradientY * gradientY);
		float line = edge > EDGE_BELOW ? Math.min(0.85F, (edge - EDGE_BELOW) * 1.5F + 0.45F) : 0.0F;
		return Math.max(silhouette, line);
	}

	private static void groundStrokes(final NativeImage image, final int[] bottom, final int spacing, final int ink, final float shown) {
		int width = image.getWidth();
		int height = image.getHeight();
		int reach = 2 + spacing * 3;
		for (int x = 0; x < width; x++) {
			if (bottom[x] < 0) {
				continue;
			}

			for (int dy = 2; dy < reach; dy++) {
				int y = bottom[x] + dy;
				if (y >= height || ARGB.alpha(image.getPixel(x, y)) > 0) {
					continue;
				}

				float fade = 1.0F - (float)dy / reach;
				if (Math.floorMod(y + (int)(smooth(x / 6.0F, 4) * 2.0F), spacing) < STROKE && hash(Math.floorDiv(x, 4), y, 11) < 0.75F * fade) {
					image.setPixel(x, y, ARGB.color(Math.round(150.0F * fade * shown), ink));
				}
			}
		}
	}

	private static float smooth(final float position, final int salt) {
		int step = Mth.floor(position);
		float blend = position - step;
		blend = blend * blend * (3.0F - 2.0F * blend);
		return Mth.lerp(blend, hash(step, salt, 0), hash(step + 1, salt, 0));
	}

	private static float hash(final int x, final int y, final int salt) {
		int hash = x * 374761393 + y * 668265263 + salt * 2147483647;
		hash = (hash ^ hash >>> 13) * 1274126177;
		return ((hash ^ hash >>> 16) & 0xFFFFFF) / 16777216.0F;
	}
}
