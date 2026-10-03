package com.khrux.commonplace.client.atlas;

import com.khrux.commonplace.client.ClientHandbook;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class AtlasTileRenderer {
	private static final int CONVEX = 0;
	private static final int CONCAVE = 1;
	private static final int HORIZONTAL = 2;
	private static final int VERTICAL = 3;
	private static final int FULL = 4;
	private static final int SINGLE_OBJECT = 5;

	public static void render(
		final AtlasTileRenderer.Output output, final int minChunkX, final int minChunkZ, final int maxChunkX, final int maxChunkZ, final int originX, final int originY, final int subtile
	) {
		for (int z = minChunkZ; z <= maxChunkZ + 1; z++) {
			for (int x = minChunkX; x <= maxChunkX + 1; x++) {
				renderCorner(output, x, z, originX + (x - minChunkX) * subtile * 2, originY + (z - minChunkZ) * subtile * 2, subtile);
			}
		}
	}

	private static void renderCorner(final AtlasTileRenderer.Output output, final int x, final int z, final int screenX, final int screenY, final int subtile) {
		AtlasTileTexture topLeft = ClientHandbook.get().atlas().getTexture(x - 1, z - 1);
		AtlasTileTexture topRight = ClientHandbook.get().atlas().getTexture(x, z - 1);
		AtlasTileTexture bottomLeft = ClientHandbook.get().atlas().getTexture(x - 1, z);
		AtlasTileTexture bottomRight = ClientHandbook.get().atlas().getTexture(x, z);
		if (topLeft != null) {
			int shape = shape(topLeft, topRight, bottomLeft, bottomRight, ClientHandbook.get().atlas().getTexture(x - 1, z - 2), ClientHandbook.get().atlas().getTexture(x - 2, z - 1));
			draw(output, topLeft, x - 1, z - 1, shape, 1, 1, screenX - subtile, screenY - subtile, subtile);
		}

		if (topRight != null) {
			int shape = shape(topRight, topLeft, bottomRight, bottomLeft, ClientHandbook.get().atlas().getTexture(x, z - 2), ClientHandbook.get().atlas().getTexture(x + 1, z - 1));
			draw(output, topRight, x, z - 1, shape, 0, 1, screenX, screenY - subtile, subtile);
		}

		if (bottomLeft != null) {
			int shape = shape(bottomLeft, bottomRight, topLeft, topRight, ClientHandbook.get().atlas().getTexture(x - 1, z + 1), ClientHandbook.get().atlas().getTexture(x - 2, z));
			draw(output, bottomLeft, x - 1, z, shape, 1, 0, screenX - subtile, screenY, subtile);
		}

		if (bottomRight != null) {
			int shape = shape(bottomRight, bottomLeft, topRight, topLeft, ClientHandbook.get().atlas().getTexture(x, z + 1), ClientHandbook.get().atlas().getTexture(x + 1, z));
			draw(output, bottomRight, x, z, shape, 0, 0, screenX, screenY, subtile);
		}
	}

	private static int shape(
		final AtlasTileTexture tile,
		final @Nullable AtlasTileTexture across,
		final @Nullable AtlasTileTexture along,
		final @Nullable AtlasTileTexture diagonal,
		final @Nullable AtlasTileTexture beyondAlong,
		final @Nullable AtlasTileTexture beyondAcross
	) {
		int shape = CONVEX;
		if (across != null && tile.tilesHorizontally(across)) {
			shape = HORIZONTAL;
		}

		if (along != null && tile.tilesVertically(along)) {
			shape = shape == HORIZONTAL ? CONCAVE : VERTICAL;
			if (shape == CONCAVE && diagonal != null && tile.tiles(diagonal)) {
				shape = FULL;
			}
		}

		boolean joinsBeyond = beyondAlong != null && tile.tilesVertically(beyondAlong) || beyondAcross != null && tile.tilesHorizontally(beyondAcross);
		return shape == CONVEX && !joinsBeyond ? SINGLE_OBJECT : shape;
	}

	private static void draw(
		final AtlasTileRenderer.Output output,
		final AtlasTileTexture texture,
		final int chunkX,
		final int chunkZ,
		final int shape, final int partU, final int partV, final int x, final int y, final int subtile
	) {
		int u = switch (shape) {
			case CONVEX, VERTICAL -> partU * 3;
			case CONCAVE -> 2 + partU;
			case HORIZONTAL, FULL -> 2 - partU;
			default -> partU;
		};
		int v = switch (shape) {
			case CONVEX, HORIZONTAL -> 2 + partV * 3;
			case CONCAVE, SINGLE_OBJECT -> partV;
			default -> 4 - partV;
		};
		output.subtile(texture.getLocation(), chunkX, chunkZ, x, y, subtile, u / 4.0F, (u + 1) / 4.0F, v / 6.0F, (v + 1) / 6.0F);
	}

	public interface Output {
		void subtile(Identifier texture, int chunkX, int chunkZ, int x, int y, int size, float u0, float u1, float v0, float v1);
	}
}
