package com.khrux.handbook.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

public class InkMasks {
	private static final int SHAPE_REGION = 8;
	private static final Map<Identifier, Optional<InkMasks.Mask>> MASKS = new HashMap<>();

	public static InkMasks.@Nullable Mask get(final Identifier texture, final float inkBelow) {
		return MASKS.computeIfAbsent(texture, location -> create(location, inkBelow)).orElse(null);
	}

	public static void clear() {
		Minecraft minecraft = Minecraft.getInstance();
		MASKS.values().forEach(mask -> mask.ifPresent(present -> minecraft.getTextureManager().release(present.location())));
		MASKS.clear();
	}

	private static Optional<InkMasks.Mask> create(final Identifier texture, final float inkBelow) {
		Minecraft minecraft = Minecraft.getInstance();
		Optional<Resource> resource = minecraft.getResourceManager().getResource(texture);
		if (resource.isEmpty()) {
			return Optional.empty();
		}

		NativeImage image;
		try (InputStream input = resource.get().open()) {
			image = NativeImage.read(input);
		} catch (IOException e) {
			return Optional.empty();
		}

		int width = image.getWidth();
		int height = image.getHeight();
		int[] original = new int[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int color = image.getPixel(x, y);
				original[x + y * width] = color;
				float luminance = (ARGB.red(color) * 0.299F + ARGB.green(color) * 0.587F + ARGB.blue(color) * 0.114F) / 255.0F;
				image.setPixel(x, y, luminance < inkBelow && ARGB.alpha(color) > 0 ? ARGB.color(ARGB.alpha(color), 0xFFFFFF) : 0);
			}
		}

		int[] shapes = new int[width * height];
		Arrays.fill(shapes, -1);
		IntList centerX = new IntArrayList();
		IntList centerY = new IntArrayList();
		IntArrayList queue = new IntArrayList();
		for (int start = 0; start < shapes.length; start++) {
			if (shapes[start] != -1 || image.getPixel(start % width, start / width) == 0) {
				continue;
			}

			int shape = centerX.size();
			int sumX = 0;
			int sumY = 0;
			queue.clear();
			queue.add(start);
			shapes[start] = shape;
			for (int i = 0; i < queue.size(); i++) {
				int index = queue.getInt(i);
				int x = index % width;
				int y = index / width;
				sumX += x;
				sumY += y;
				for (int dy = -1; dy <= 1; dy++) {
					for (int dx = -1; dx <= 1; dx++) {
						int nx = x + dx;
						int ny = y + dy;
						if (nx < 0 || ny < 0 || nx >= width || ny >= height || nx / SHAPE_REGION != x / SHAPE_REGION || ny / SHAPE_REGION != y / SHAPE_REGION) {
							continue;
						}

						int neighbour = nx + ny * width;
						if (shapes[neighbour] == -1 && image.getPixel(nx, ny) != 0) {
							shapes[neighbour] = shape;
							queue.add(neighbour);
						}
					}
				}
			}

			centerX.add(sumX / queue.size());
			centerY.add(sumY / queue.size());
		}

		Identifier location = texture.withPath(path -> path.replace(".png", "_ink"));
		minecraft.getTextureManager().register(location, new DynamicTexture(location::toString, image));
		return Optional.of(new InkMasks.Mask(location, width, height, original, shapes, centerX.toIntArray(), centerY.toIntArray()));
	}

	public record Mask(Identifier location, int width, int height, int[] original, int[] shapes, int[] centerX, int[] centerY) {
		public int getShape(final int x, final int y) {
			return x < 0 || y < 0 || x >= this.width || y >= this.height ? -1 : this.shapes[x + y * this.width];
		}
	}
}
