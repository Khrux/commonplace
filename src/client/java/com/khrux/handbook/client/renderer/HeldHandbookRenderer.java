package com.khrux.handbook.client.renderer;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.client.ClientNotebook;
import com.khrux.handbook.client.ClientSheen;
import com.khrux.handbook.client.atlas.AtlasTextures;
import com.khrux.handbook.client.atlas.AtlasTileRenderer;
import com.khrux.handbook.client.atlas.ClientAtlas;
import com.khrux.handbook.client.gui.components.AtlasMap;
import com.khrux.handbook.client.gui.components.NoteCanvas;
import com.khrux.handbook.client.gui.screens.HandbookScreen;
import com.khrux.handbook.client.gui.screens.HandbookTab;
import com.khrux.handbook.client.gui.screens.NotesPage;
import com.khrux.handbook.client.gui.screens.RecipesPage;
import com.khrux.handbook.world.entity.player.NotePage;
import com.khrux.handbook.world.item.HandbookItem;
import com.khrux.handbook.world.level.atlas.AtlasMarker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

public class HeldHandbookRenderer {
	private static final RenderType BOOK = RenderTypes.text(Handbook.id("textures/gui/book.png"));
	private static final RenderType COVER = RenderTypes.text(HandbookScreen.COVER_LOCATION);
	private static final RenderType INK = RenderTypes.text(Handbook.id("textures/gui/ink.png"));
	private static final float HELD_MARKER_SIZE = 16.0F;
	private static final RenderType ATLAS_PLAYER = RenderTypes.text(Handbook.id("textures/gui/atlas/player.png"));
	private static final int MAP_LEFT = 14;
	private static final int MAP_TOP = 7;
	private static final int MAP_RIGHT = 281;
	private static final int MAP_BOTTOM = 173;
	private static final int HELD_SUBTILE = 4;
	private static final float BOOK_WIDTH = 295.0F;
	private static final float BOOK_HEIGHT = 180.0F;
	private static final float MAP_SIZE = 142.0F;
	private static final int LEFT_PAGE_X = 13;
	private static final int RIGHT_PAGE_X = 153;
	private static final int PAGE_Y = 7;
	private static final int PAGE_WIDTH = 128;
	private static final float SEAM = 147.0F;
	private static final float TURNING_PAGE_WIDTH = 141.0F;
	private static final float TURNING_PAGE_TOP = 6.0F;
	private static final float TURNING_PAGE_BOTTOM = 174.0F;
	private static final long TURN_MILLIS = 350L;
	private static final int DIMMED_TILE_COLOR = 0x50FFFFFF;
	private static long turnStart = -TURN_MILLIS;
	private static int turnDirection;
	private static HandbookTab previousTab = HandbookTab.RECIPES;

	public static void turnPage(final HandbookTab from, final int direction) {
		long elapsed = Util.getMillis() - turnStart;
		previousTab = elapsed < TURN_MILLIS ? previousTab : from;
		turnStart = Util.getMillis();
		turnDirection = direction;
	}

	public static void render(final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int lightCoords) {
		poseStack.rotateDegrees(Axis.YP, 180.0F);
		poseStack.rotateDegrees(Axis.ZP, 180.0F);
		poseStack.scale(0.38F, 0.38F, 0.38F);
		poseStack.translate(-0.5F, -0.5F, 0.0F);
		poseStack.scale(0.0078125F, 0.0078125F, 0.0078125F);
		float scale = MAP_SIZE / BOOK_WIDTH;
		poseStack.translate(-7.0F, 64.0F - BOOK_HEIGHT * scale / 2.0F, 0.0F);
		poseStack.scale(scale, scale, 1.0F);
		renderBook(poseStack, submitNodeCollector, BOOK, lightCoords, -1);
		poseStack.translate(0.0F, 0.0F, -0.005F);
		renderBook(poseStack, submitNodeCollector.order(1), COVER, lightCoords, HandbookItem.getColor(Minecraft.getInstance().player));
		poseStack.translate(0.0F, 0.0F, -0.02F);
		OrderedSubmitNodeCollector pages = submitNodeCollector.order(2);
		HandbookTab current = HandbookScreen.getLastTab();
		long elapsed = Util.getMillis() - turnStart;
		if (elapsed >= TURN_MILLIS || previousTab == current) {
			renderSide(poseStack, pages, lightCoords, current, true);
			renderSide(poseStack, pages, lightCoords, current, false);
			return;
		}

		boolean forward = turnDirection > 0;
		renderSide(poseStack, pages, lightCoords, forward ? previousTab : current, true);
		renderSide(poseStack, pages, lightCoords, forward ? current : previousTab, false);
		float progress = Mth.sin(elapsed / (float)TURN_MILLIS * Mth.HALF_PI);
		float angle = (forward ? progress : 1.0F - progress) * Mth.PI;
		boolean front = angle < Mth.HALF_PI;
		poseStack.pushPose();
		poseStack.scale(1.0F, 1.0F, scale);
		poseStack.translate(SEAM, 0.0F, 0.0F);
		poseStack.rotateDegrees(Axis.YP, angle * Mth.RAD_TO_DEG);
		if (!front) {
			poseStack.scale(-1.0F, 1.0F, 1.0F);
		}

		poseStack.translate(-SEAM, 0.0F, 0.0F);
		int shade = (int)(255.0F - Mth.sin(angle) * 60.0F);
		renderTurningPage(poseStack, submitNodeCollector.order(3), lightCoords, front, ARGB.color(255, shade, shade, shade));
		poseStack.translate(0.0F, 0.0F, front ? -0.02F : 0.02F);
		renderSide(poseStack, submitNodeCollector.order(4), lightCoords, front ? (forward ? previousTab : current) : (forward ? current : previousTab), !front);
		poseStack.popPose();
	}

	private static void renderBook(final PoseStack poseStack, final OrderedSubmitNodeCollector submitNodeCollector, final RenderType renderType, final int lightCoords, final int color) {
		submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
			float u0 = HandbookScreen.COVER_LEFT / (float)HandbookScreen.WIDTH;
			float v0 = HandbookScreen.COVER_TOP / (float)HandbookScreen.HEIGHT;
			float u1 = (HandbookScreen.COVER_LEFT + BOOK_WIDTH) / HandbookScreen.WIDTH;
			float v1 = (HandbookScreen.COVER_TOP + BOOK_HEIGHT) / HandbookScreen.HEIGHT;
			buffer.addVertex(pose, 0.0F, BOOK_HEIGHT, 0.0F).setColor(color).setUv(u0, v1).setLight(lightCoords);
			buffer.addVertex(pose, BOOK_WIDTH, BOOK_HEIGHT, 0.0F).setColor(color).setUv(u1, v1).setLight(lightCoords);
			buffer.addVertex(pose, BOOK_WIDTH, 0.0F, 0.0F).setColor(color).setUv(u1, v0).setLight(lightCoords);
			buffer.addVertex(pose, 0.0F, 0.0F, 0.0F).setColor(color).setUv(u0, v0).setLight(lightCoords);
		});
	}

	private static void renderTurningPage(final PoseStack poseStack, final OrderedSubmitNodeCollector submitNodeCollector, final int lightCoords, final boolean front, final int color) {
		float x0 = front ? SEAM : SEAM - TURNING_PAGE_WIDTH;
		float x1 = front ? SEAM + TURNING_PAGE_WIDTH : SEAM;
		float u0 = (HandbookScreen.COVER_LEFT + x0) / HandbookScreen.WIDTH;
		float u1 = (HandbookScreen.COVER_LEFT + x1) / HandbookScreen.WIDTH;
		float v0 = (HandbookScreen.COVER_TOP + TURNING_PAGE_TOP) / HandbookScreen.HEIGHT;
		float v1 = (HandbookScreen.COVER_TOP + TURNING_PAGE_BOTTOM) / HandbookScreen.HEIGHT;
		submitNodeCollector.submitCustomGeometry(poseStack, BOOK, (pose, buffer) -> {
			buffer.addVertex(pose, x0, TURNING_PAGE_BOTTOM, 0.0F).setColor(color).setUv(u0, v1).setLight(lightCoords);
			buffer.addVertex(pose, x1, TURNING_PAGE_BOTTOM, 0.0F).setColor(color).setUv(u1, v1).setLight(lightCoords);
			buffer.addVertex(pose, x1, TURNING_PAGE_TOP, 0.0F).setColor(color).setUv(u1, v0).setLight(lightCoords);
			buffer.addVertex(pose, x0, TURNING_PAGE_TOP, 0.0F).setColor(color).setUv(u0, v0).setLight(lightCoords);
			buffer.addVertex(pose, x0, TURNING_PAGE_TOP, 0.0F).setColor(color).setUv(u0, v0).setLight(lightCoords);
			buffer.addVertex(pose, x1, TURNING_PAGE_TOP, 0.0F).setColor(color).setUv(u1, v0).setLight(lightCoords);
			buffer.addVertex(pose, x1, TURNING_PAGE_BOTTOM, 0.0F).setColor(color).setUv(u1, v1).setLight(lightCoords);
			buffer.addVertex(pose, x0, TURNING_PAGE_BOTTOM, 0.0F).setColor(color).setUv(u0, v1).setLight(lightCoords);
		});
	}

	private static void renderSide(final PoseStack poseStack, final OrderedSubmitNodeCollector submitNodeCollector, final int lightCoords, final HandbookTab tab, final boolean left) {
		switch (tab) {
			case ATLAS -> renderAtlas(poseStack, submitNodeCollector, lightCoords, left ? MAP_LEFT : SEAM, left ? SEAM : MAP_RIGHT);
			case NOTES -> {
				int page = NotesPage.getSpread() * 2 + (left ? 0 : 1);
				renderNotePage(poseStack, submitNodeCollector, lightCoords, ClientNotebook.get(NotesPage.getBook(), page), left ? LEFT_PAGE_X : RIGHT_PAGE_X);
			}
			case ENDER, RECIPES, FIELD_GUIDE -> {
				Identifier snapshot = PageSnapshot.get(tab);
				if (snapshot != null) {
					renderSnapshot(poseStack, submitNodeCollector, lightCoords, snapshot, left);
				} else if (left) {
					renderHeading(poseStack, submitNodeCollector, lightCoords, tab.getName(), LEFT_PAGE_X);
				}
			}
		}
	}

	private static void renderSnapshot(final PoseStack poseStack, final OrderedSubmitNodeCollector submitNodeCollector, final int lightCoords, final Identifier snapshot, final boolean left) {
		float pageLeft = PageSnapshot.LEFT - HandbookScreen.COVER_LEFT;
		float pageTop = PageSnapshot.TOP - HandbookScreen.COVER_TOP;
		float half = (SEAM - pageLeft) / PageSnapshot.WIDTH;
		float x0 = left ? pageLeft : SEAM;
		float x1 = left ? SEAM : pageLeft + PageSnapshot.WIDTH;
		float u0 = left ? 0.0F : half;
		float u1 = left ? half : 1.0F;
		float y0 = pageTop;
		float y1 = pageTop + PageSnapshot.HEIGHT;
		submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.text(snapshot), (pose, buffer) -> {
			buffer.addVertex(pose, x0, y1, 0.0F).setColor(-1).setUv(u0, 0.0F).setLight(lightCoords);
			buffer.addVertex(pose, x1, y1, 0.0F).setColor(-1).setUv(u1, 0.0F).setLight(lightCoords);
			buffer.addVertex(pose, x1, y0, 0.0F).setColor(-1).setUv(u1, 1.0F).setLight(lightCoords);
			buffer.addVertex(pose, x0, y0, 0.0F).setColor(-1).setUv(u0, 1.0F).setLight(lightCoords);
		});
	}

	private static void renderAtlas(final PoseStack poseStack, final OrderedSubmitNodeCollector submitNodeCollector, final int lightCoords, final float minX, final float maxX) {
		Player player = Minecraft.getInstance().player;
		double blocksPerUnit = 16.0 / (HELD_SUBTILE * 2);
		double leftBlock = player.getX() - (MAP_RIGHT - MAP_LEFT) / 2.0 * blocksPerUnit;
		double topBlock = player.getZ() - (MAP_BOTTOM - MAP_TOP) / 2.0 * blocksPerUnit;
		int minChunkX = Mth.floor(leftBlock / 16.0) - 1;
		int minChunkZ = Mth.floor(topBlock / 16.0) - 1;
		int maxChunkX = Mth.floor((leftBlock + (MAP_RIGHT - MAP_LEFT) * blocksPerUnit) / 16.0) + 1;
		int maxChunkZ = Mth.floor((topBlock + (MAP_BOTTOM - MAP_TOP) * blocksPerUnit) / 16.0) + 1;
		int originX = MAP_LEFT + (int)Math.round((minChunkX * 16 - leftBlock) / blocksPerUnit);
		int originY = MAP_TOP + (int)Math.round((minChunkZ * 16 - topBlock) / blocksPerUnit);
		String dimension = player.level().dimension().identifier().toString();
		Map<Identifier, List<HeldHandbookRenderer.Quad>> subtiles = new HashMap<>();
		AtlasTileRenderer.render((texture, chunkX, chunkZ, x, y, size, u0, u1, v0, v1) -> {
			float x0 = Math.max(x, minX);
			float x1 = Math.min(x + size, maxX);
			if (x1 > x0 && x >= MAP_LEFT && y >= MAP_TOP && x + size <= MAP_RIGHT && y + size <= MAP_BOTTOM) {
				float clippedU0 = u0 + (u1 - u0) * (x0 - x) / size;
				float clippedU1 = u0 + (u1 - u0) * (x1 - x) / size;
				int sheen = ClientSheen.getChunkSheen(dimension, ChunkPos.pack(chunkX, chunkZ));
				InkMasks.Mask mask = sheen == ClientSheen.NONE || sheen == ClientSheen.DIMMED ? null : InkMasks.get(texture, AtlasMap.INK_BELOW);
				Identifier drawn = mask == null ? texture : mask.location();
				int color = mask != null ? AtlasMap.getInkColor(sheen) : sheen == ClientSheen.DIMMED ? DIMMED_TILE_COLOR : -1;
				subtiles.computeIfAbsent(drawn, key -> new ArrayList<>()).add(new HeldHandbookRenderer.Quad(x0, y, x1, y + size, clippedU0, clippedU1, v0, v1, color));
			}
		}, minChunkX, minChunkZ, maxChunkX, maxChunkZ, originX, originY, HELD_SUBTILE);
		subtiles.forEach((texture, quads) -> submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.text(texture), (pose, buffer) -> {
			for (HeldHandbookRenderer.Quad quad : quads) {
				buffer.addVertex(pose, quad.x0(), quad.y1(), 0.0F).setColor(quad.color()).setUv(quad.u0(), quad.v1()).setLight(lightCoords);
				buffer.addVertex(pose, quad.x1(), quad.y1(), 0.0F).setColor(quad.color()).setUv(quad.u1(), quad.v1()).setLight(lightCoords);
				buffer.addVertex(pose, quad.x1(), quad.y0(), 0.0F).setColor(quad.color()).setUv(quad.u1(), quad.v0()).setLight(lightCoords);
				buffer.addVertex(pose, quad.x0(), quad.y0(), 0.0F).setColor(quad.color()).setUv(quad.u0(), quad.v0()).setLight(lightCoords);
			}
		}));
		AtlasTextures textures = AtlasTextures.get(Minecraft.getInstance().getResourceManager());
		for (Long2ObjectMap.Entry<String> entry : ClientAtlas.getStructures().long2ObjectEntrySet()) {
			AtlasTextures.StructureMarker marker = textures.getStructureMarker(entry.getValue());
			if (marker == null) {
				continue;
			}

			ChunkPos chunk = ChunkPos.unpack(entry.getLongKey());
			float x = (float)(MAP_LEFT + (chunk.getMiddleBlockX() - leftBlock) / blocksPerUnit) + marker.offsetX() / 2.0F;
			float y = (float)(MAP_TOP + (chunk.getMiddleBlockZ() - topBlock) / blocksPerUnit) + marker.offsetY() / 2.0F;
			float width = marker.width() / 2.0F;
			float height = marker.height() / 2.0F;
			float center = x + width / 2.0F;
			if (center < minX || center >= maxX || x < MAP_LEFT || y < MAP_TOP || x + width > MAP_RIGHT || y + height > MAP_BOTTOM) {
				continue;
			}

			float u1 = (float)marker.width() / marker.textureWidth();
			float v1 = (float)marker.height() / marker.textureHeight();
			poseStack.pushPose();
			poseStack.translate(0.0F, 0.0F, -0.004F);
			submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.text(marker.texture()), (pose, buffer) -> {
				buffer.addVertex(pose, x, y + height, 0.0F).setColor(-1).setUv(0.0F, v1).setLight(lightCoords);
				buffer.addVertex(pose, x + width, y + height, 0.0F).setColor(-1).setUv(u1, v1).setLight(lightCoords);
				buffer.addVertex(pose, x + width, y, 0.0F).setColor(-1).setUv(u1, 0.0F).setLight(lightCoords);
				buffer.addVertex(pose, x, y, 0.0F).setColor(-1).setUv(0.0F, 0.0F).setLight(lightCoords);
			});
			poseStack.popPose();
		}

		for (AtlasMarker marker : ClientAtlas.getMarkers()) {
			float x = (float)(MAP_LEFT + (marker.x() + 0.5 - leftBlock) / blocksPerUnit) - marker.type().getAnchorX() / 2.0F;
			float y = (float)(MAP_TOP + (marker.z() + 0.5 - topBlock) / blocksPerUnit) - marker.type().getAnchorY() / 2.0F;
			float center = x + HELD_MARKER_SIZE / 2.0F;
			if (center >= minX && center < maxX && x >= MAP_LEFT && y >= MAP_TOP && x + HELD_MARKER_SIZE <= MAP_RIGHT && y + HELD_MARKER_SIZE <= MAP_BOTTOM) {
				poseStack.pushPose();
				poseStack.translate(0.0F, 0.0F, -0.005F);
				quad(poseStack, submitNodeCollector, RenderTypes.text(marker.type().getTexture()), x, y, x + HELD_MARKER_SIZE, y + HELD_MARKER_SIZE, lightCoords, -1);
				quad(poseStack, submitNodeCollector, RenderTypes.text(marker.type().getAccentTexture()), x, y, x + HELD_MARKER_SIZE, y + HELD_MARKER_SIZE, lightCoords, marker.type().getAccentColor());
				poseStack.popPose();
			}
		}

		float playerX = (MAP_LEFT + MAP_RIGHT) / 2.0F;
		if (playerX >= minX && playerX < maxX) {
			poseStack.pushPose();
			poseStack.translate(playerX, (MAP_TOP + MAP_BOTTOM) / 2.0F, -0.01F);
			poseStack.rotateDegrees(Axis.ZP, player.getYRot() + 180.0F);
			quad(poseStack, submitNodeCollector, ATLAS_PLAYER, -3.5F, -4.0F, 3.5F, 4.0F, lightCoords, -1);
			poseStack.popPose();
		}
	}

	private static void quad(
		final PoseStack poseStack, final OrderedSubmitNodeCollector submitNodeCollector, final RenderType renderType, final float x0, final float y0, final float x1, final float y1, final int lightCoords,
		final int color
	) {
		submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
			buffer.addVertex(pose, x0, y1, 0.0F).setColor(color).setUv(0.0F, 1.0F).setLight(lightCoords);
			buffer.addVertex(pose, x1, y1, 0.0F).setColor(color).setUv(1.0F, 1.0F).setLight(lightCoords);
			buffer.addVertex(pose, x1, y0, 0.0F).setColor(color).setUv(1.0F, 0.0F).setLight(lightCoords);
			buffer.addVertex(pose, x0, y0, 0.0F).setColor(color).setUv(0.0F, 0.0F).setLight(lightCoords);
		});
	}

	private static void renderNotePage(
		final PoseStack poseStack, final OrderedSubmitNodeCollector submitNodeCollector, final int lightCoords, final NotePage notePage, final int x
	) {
		if (notePage.drawing().length > 0) {
			submitNodeCollector.submitCustomGeometry(poseStack, INK, (pose, buffer) -> {
				for (int py = 0; py < NotePage.CANVAS_HEIGHT; py++) {
					for (int px = 0; px < NotePage.CANVAS_WIDTH; px++) {
						int color = notePage.getPixel(px, py);
						if (color == 0) {
							continue;
						}

						int ink = NoteCanvas.getInkColor(color);
						float x0 = x + px * 2;
						float y0 = PAGE_Y + py * 2;
						buffer.addVertex(pose, x0, y0 + 2.0F, 0.0F).setColor(ink).setUv(0.0F, 1.0F).setLight(lightCoords);
						buffer.addVertex(pose, x0 + 2.0F, y0 + 2.0F, 0.0F).setColor(ink).setUv(1.0F, 1.0F).setLight(lightCoords);
						buffer.addVertex(pose, x0 + 2.0F, y0, 0.0F).setColor(ink).setUv(1.0F, 0.0F).setLight(lightCoords);
						buffer.addVertex(pose, x0, y0, 0.0F).setColor(ink).setUv(0.0F, 0.0F).setLight(lightCoords);
					}
				}
			});
		}

		Font font = Minecraft.getInstance().font;
		poseStack.pushPose();
		poseStack.translate(0.0F, 0.0F, -0.01F);
		int y = PAGE_Y + 4;
		for (FormattedCharSequence line : font.split(FormattedText.of(notePage.text()), PAGE_WIDTH - 8)) {
			submitNodeCollector.submitText(poseStack, x + 4, y, line, false, Font.DisplayMode.NORMAL, lightCoords, RecipesPage.INK_COLOR, 0, 0);
			y += font.lineHeight;
		}

		poseStack.popPose();
	}

	private static void renderHeading(
		final PoseStack poseStack, final OrderedSubmitNodeCollector submitNodeCollector, final int lightCoords, final Component heading, final int x
	) {
		Font font = Minecraft.getInstance().font;
		float width = font.width(heading);
		submitNodeCollector.submitText(
			poseStack, x + (PAGE_WIDTH - width) / 2.0F, PAGE_Y + 8, heading.getVisualOrderText(), false, Font.DisplayMode.NORMAL, lightCoords, RecipesPage.INK_COLOR, 0, 0
		);
	}

	private record Quad(float x0, float y0, float x1, float y1, float u0, float u1, float v0, float v1, int color) {
	}
}
