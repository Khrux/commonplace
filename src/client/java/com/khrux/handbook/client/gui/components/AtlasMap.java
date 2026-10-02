package com.khrux.handbook.client.gui.components;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.client.ClientSheen;
import com.khrux.handbook.client.atlas.AtlasTextures;
import com.khrux.handbook.client.atlas.AtlasTileRenderer;
import com.khrux.handbook.client.atlas.ClientAtlas;
import com.khrux.handbook.client.gui.screens.RecipesPage;
import com.khrux.handbook.client.renderer.InkMasks;
import com.khrux.handbook.network.protocol.PlaceAtlasMarkerPayload;
import com.khrux.handbook.network.protocol.RemoveAtlasMarkerPayload;
import com.khrux.handbook.world.level.atlas.AtlasMarker;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public class AtlasMap extends AbstractWidget {
	private static final float INK_DEPTH = 0.2F;
	public static final float INK_BELOW = 0.35F;
	private static final int SUBTILE_TEXELS = 8;
	private static final int CHUNK_TEXELS = 16;
	private static final float BLEED_TEXELS = 7.0F;
	private static final float WOBBLE_TEXELS = 6.0F;
	private static final float WOBBLE_PERIOD = 9.0F;
	private static final int DIMMED_ALPHA = 112;
	private static final Identifier RASTER_LOCATION = Handbook.id("atlas_raster");
	private static final Identifier PLAYER_LOCATION = Handbook.id("textures/gui/atlas/player.png");
	private static final int[] ZOOM_LEVELS = {1, 2, 3, 4, 5, 6, 8, 10, 12, 16};
	private static final Identifier REMOVE_LOCATION = Handbook.id("textures/gui/atlas/del_marker.png");
	private static final int MARKER_TEXTURE_SIZE = 32;
	private static final int NEAR_CLIP_SUBTILE = 8;
	private static final int MARKER_PADDING = 8;
	private static final int MARKER_SIZE = 16;
	private static final int CELL = 18;
	private static final int COLUMNS = 6;
	private static final int CARD_WIDTH = COLUMNS * CELL + 8;
	private static final int CARD_HEIGHT = 2 * CELL + 24;
	private static final int REMOVE_WIDTH = 74;
	private static final int REMOVE_HEIGHT = 22;
	private static final int CARD_FILL = 0xFFF1E2BE;
	private static final int CARD_EDGE = 0xFF8A5F38;
	private static final int HIGHLIGHT = 0x303F2A1C;
	private static final int HINT_COLOR = 0x903F2A1C;
	private static int subtile = 4;
	private static @Nullable DynamicTexture raster;
	private static AtlasMap.@Nullable RasterKey rasterKey;
	private double centerX;
	private double centerZ;
	private @Nullable EditBox label;
	private @Nullable AtlasMarker removing;
	private int cardX;
	private int cardY;
	private int markerX;
	private int markerZ;

	public AtlasMap(final int x, final int y, final int width, final int height) {
		super(x, y, width, height, Component.translatable("handbook.tab.atlas"));
		Player player = Minecraft.getInstance().player;
		this.centerX = player.getX();
		this.centerZ = player.getZ();
		AtlasTextures.reload(Minecraft.getInstance().getResourceManager());
		rasterKey = null;
	}

	private double blocksPerPixel() {
		return 16.0 / (subtile * 2);
	}

	@Override
	protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		double scale = this.blocksPerPixel();
		double leftBlock = this.centerX - this.width / 2.0 * scale;
		double topBlock = this.centerZ - this.height / 2.0 * scale;
		int minChunkX = Mth.floor(leftBlock / 16.0) - 1;
		int minChunkZ = Mth.floor(topBlock / 16.0) - 1;
		int maxChunkX = Mth.floor((leftBlock + this.width * scale) / 16.0) + 1;
		int maxChunkZ = Mth.floor((topBlock + this.height * scale) / 16.0) + 1;
		int originX = this.getX() + (int)Math.round((minChunkX * 16 - leftBlock) / scale);
		int originY = this.getY() + (int)Math.round((minChunkZ * 16 - topBlock) / scale);
		graphics.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);
		String dimension = Minecraft.getInstance().level.dimension().identifier().toString();
		AtlasMap.RasterKey key = new AtlasMap.RasterKey(
			dimension, minChunkX, minChunkZ, maxChunkX, maxChunkZ, originX - this.getX(), originY - this.getY(), subtile, ClientAtlas.getVersion(), ClientSheen.getVersion(), ClientSheen.getHighlighted()
		);
		if (!key.equals(rasterKey) || raster == null) {
			this.rasterize(key);
			rasterKey = key;
		}

		graphics.blit(RenderPipelines.GUI_TEXTURED, RASTER_LOCATION, this.getX(), this.getY(), 0.0F, 0.0F, this.width, this.height, this.width, this.height);
		this.extractStructureMarkers(graphics, leftBlock, topBlock, scale);
		AtlasMarker hovered = this.extractMarkers(graphics, leftBlock, topBlock, scale, mouseX, mouseY);
		this.extractPlayer(graphics, leftBlock, topBlock, scale);
		graphics.disableScissor();
		if (this.label != null) {
			this.extractPlaceCard(graphics, mouseX, mouseY, a);
		} else if (this.removing != null) {
			this.extractRemoveCard(graphics, mouseX, mouseY);
		} else if (hovered != null) {
			graphics.setTooltipForNextFrame(Minecraft.getInstance().font, markerName(hovered), mouseX, mouseY);
		}
	}

	private void rasterize(final AtlasMap.RasterKey key) {
		if (raster == null || raster.getPixels().getWidth() != this.width || raster.getPixels().getHeight() != this.height) {
			if (raster != null) {
				Minecraft.getInstance().getTextureManager().release(RASTER_LOCATION);
			}

			raster = new DynamicTexture(RASTER_LOCATION::toString, this.width, this.height, true);
			Minecraft.getInstance().getTextureManager().register(RASTER_LOCATION, raster);
		}

		NativeImage image = raster.getPixels();
		image.fillRect(0, 0, this.width, this.height, 0);
		Long2IntMap sheens = new Long2IntOpenHashMap();
		for (int chunkZ = key.minChunkZ() - 1; chunkZ <= key.maxChunkZ() + 1; chunkZ++) {
			for (int chunkX = key.minChunkX() - 1; chunkX <= key.maxChunkX() + 1; chunkX++) {
				long pos = ChunkPos.pack(chunkX, chunkZ);
				sheens.put(pos, ClientAtlas.getTexture(chunkX, chunkZ) == null ? ClientSheen.NONE : ClientSheen.getChunkSheen(key.dimension(), pos));
			}
		}

		AtlasTileRenderer.render(
			(texture, chunkX, chunkZ, x, y, size, u0, u1, v0, v1) -> {
				int localX = x - (key.originX() + (chunkX - key.minChunkX()) * size * 2) == 0 ? 0 : SUBTILE_TEXELS;
				int localZ = y - (key.originY() + (chunkZ - key.minChunkZ()) * size * 2) == 0 ? 0 : SUBTILE_TEXELS;
				this.rasterizeSubtile(image, texture, sheens, chunkX, chunkZ, localX, localZ, x, y, size, u0, v0);
			},
			key.minChunkX(),
			key.minChunkZ(),
			key.maxChunkX(),
			key.maxChunkZ(),
			key.originX(),
			key.originY(),
			key.subtile()
		);
		raster.upload();
	}

	private void rasterizeSubtile(
		final NativeImage image,
		final Identifier texture,
		final Long2IntMap sheens,
		final int chunkX,
		final int chunkZ,
		final int localX,
		final int localZ,
		final int x,
		final int y,
		final int size,
		final float u0,
		final float v0
	) {
		InkMasks.Mask mask = InkMasks.get(texture, INK_BELOW);
		if (mask == null) {
			return;
		}

		int regionX = Math.round(u0 * mask.width());
		int regionZ = Math.round(v0 * mask.height());
		int sheen = sheens.get(ChunkPos.pack(chunkX, chunkZ));
		int[] neighbours = {
			sheens.get(ChunkPos.pack(chunkX - 1, chunkZ)),
			sheens.get(ChunkPos.pack(chunkX + 1, chunkZ)),
			sheens.get(ChunkPos.pack(chunkX, chunkZ - 1)),
			sheens.get(ChunkPos.pack(chunkX, chunkZ + 1))
		};
		boolean border = neighbours[0] != sheen || neighbours[1] != sheen || neighbours[2] != sheen || neighbours[3] != sheen;
		for (int py = Math.max(0, y); py < Math.min(this.height, y + size); py++) {
			int texelZ = (py - y) * SUBTILE_TEXELS / size;
			for (int px = Math.max(0, x); px < Math.min(this.width, x + size); px++) {
				int texelX = (px - x) * SUBTILE_TEXELS / size;
				int color = mask.original()[regionX + texelX + (regionZ + texelZ) * mask.width()];
				if (ARGB.alpha(color) == 0) {
					continue;
				}

				int shape = mask.getShape(regionX + texelX, regionZ + texelZ);
				int chosen = sheen;
				if (border) {
					float centerX = shape >= 0 ? localX + mask.centerX()[shape] - regionX + 0.5F : localX + texelX + 0.5F;
					float centerZ = shape >= 0 ? localZ + mask.centerY()[shape] - regionZ + 0.5F : localZ + texelZ + 0.5F;
					chosen = chooseSheen(sheen, neighbours, chunkX, chunkZ, centerX, centerZ);
				}

				if (chosen == ClientSheen.DIMMED) {
					color = ARGB.color(ARGB.alpha(color) * DIMMED_ALPHA / 255, color);
				} else if (chosen != ClientSheen.NONE && shape >= 0) {
					color = ARGB.color(ARGB.alpha(color), getInkColor(chosen));
				}

				image.setPixel(px, py, color);
			}
		}
	}

	private static int chooseSheen(final int sheen, final int[] neighbours, final int chunkX, final int chunkZ, final float texelX, final float texelZ) {
		int worldX = chunkX * CHUNK_TEXELS + (int)texelX;
		int worldZ = chunkZ * CHUNK_TEXELS + (int)texelZ;
		float roll = noise(worldX, worldZ);
		int chosen = sheen;
		float nearest = BLEED_TEXELS;
		for (int side = 0; side < 4; side++) {
			int neighbour = neighbours[side];
			if (neighbour == sheen) {
				continue;
			}

			float distance = side == 0 ? texelX : side == 1 ? CHUNK_TEXELS - texelX : side == 2 ? texelZ : CHUNK_TEXELS - texelZ;
			int edge = side == 0 ? chunkX * CHUNK_TEXELS : side == 1 ? (chunkX + 1) * CHUNK_TEXELS : side == 2 ? chunkZ * CHUNK_TEXELS : (chunkZ + 1) * CHUNK_TEXELS;
			float wobble = wobble(side < 2 ? worldZ : worldX, edge, side < 2) * WOBBLE_TEXELS;
			float score = distance + (side % 2 == 0 ? -wobble : wobble);
			float chance = 0.5F - score / (BLEED_TEXELS * 2.0F);
			if (distance < nearest && roll < chance) {
				chosen = neighbour;
				nearest = distance;
			}
		}

		return chosen;
	}

	public static int getInkColor(final int sheen) {
		return ARGB.srgbLerp(INK_DEPTH, sheen, 0xFF000000);
	}

	private static float noise(final int x, final int z) {
		int hash = x * 374761393 + z * 668265263;
		hash = (hash ^ hash >>> 13) * 1274126177;
		return ((hash ^ hash >>> 16) & 0xFFFFFF) / 16777216.0F;
	}

	private static float wobble(final int along, final int edge, final boolean vertical) {
		float position = along / WOBBLE_PERIOD;
		int step = Mth.floor(position);
		float blend = position - step;
		blend = blend * blend * (3.0F - 2.0F * blend);
		int salt = vertical ? edge * 31 : edge * 31 + 17;
		return Mth.lerp(blend, noise(step, salt), noise(step + 1, salt)) * 2.0F - 1.0F;
	}

	private static Component markerName(final AtlasMarker marker) {
		return marker.label().isEmpty() ? Component.translatable("handbook.marker." + marker.type().getSerializedName()) : Component.literal(marker.label());
	}

	private void extractStructureMarkers(final GuiGraphicsExtractor graphics, final double leftBlock, final double topBlock, final double scale) {
		AtlasTextures textures = AtlasTextures.get(Minecraft.getInstance().getResourceManager());
		for (Long2ObjectMap.Entry<String> entry : ClientAtlas.getStructures().long2ObjectEntrySet()) {
			AtlasTextures.StructureMarker marker = textures.getStructureMarker(entry.getValue());
			if (marker == null || marker.nearClip() && subtile >= NEAR_CLIP_SUBTILE) {
				continue;
			}

			ChunkPos chunk = ChunkPos.unpack(entry.getLongKey());
			int x = (int)Math.round(this.getX() + (chunk.getMiddleBlockX() - leftBlock) / scale) + marker.offsetX();
			int y = (int)Math.round(this.getY() + (chunk.getMiddleBlockZ() - topBlock) / scale) + marker.offsetY();
			graphics.blit(RenderPipelines.GUI_TEXTURED, marker.texture(), x, y, 0.0F, 0.0F, marker.width(), marker.height(), marker.textureWidth(), marker.textureHeight());
		}
	}

	private @Nullable AtlasMarker extractMarkers(
		final GuiGraphicsExtractor graphics, final double leftBlock, final double topBlock, final double scale, final int mouseX, final int mouseY
	) {
		AtlasMarker hovered = null;
		for (AtlasMarker marker : ClientAtlas.getMarkers()) {
			int x = (int)Math.round(this.getX() + (marker.x() + 0.5 - leftBlock) / scale) - marker.type().getAnchorX();
			int y = (int)Math.round(this.getY() + (marker.z() + 0.5 - topBlock) / scale) - marker.type().getAnchorY();
			graphics.blit(RenderPipelines.GUI_TEXTURED, marker.type().getTexture(), x, y, 0.0F, 0.0F, MARKER_TEXTURE_SIZE, MARKER_TEXTURE_SIZE, MARKER_TEXTURE_SIZE, MARKER_TEXTURE_SIZE);
			graphics.blit(RenderPipelines.GUI_TEXTURED, marker.type().getAccentTexture(), x, y, 0.0F, 0.0F, MARKER_TEXTURE_SIZE, MARKER_TEXTURE_SIZE, MARKER_TEXTURE_SIZE, MARKER_TEXTURE_SIZE, marker.type().getAccentColor());
			x += MARKER_PADDING;
			y += MARKER_PADDING;
			if (mouseX >= x && mouseY >= y && mouseX < x + MARKER_SIZE && mouseY < y + MARKER_SIZE && this.isMouseOver(mouseX, mouseY)) {
				hovered = marker;
			}
		}

		return hovered;
	}

	private static void extractIcon(final GuiGraphicsExtractor graphics, final AtlasMarker.Type type, final int x, final int y) {
		int padding = (MARKER_TEXTURE_SIZE - CELL) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, type.getTexture(), x, y, padding, padding, CELL, CELL, MARKER_TEXTURE_SIZE, MARKER_TEXTURE_SIZE);
		graphics.blit(RenderPipelines.GUI_TEXTURED, type.getAccentTexture(), x, y, padding, padding, CELL, CELL, MARKER_TEXTURE_SIZE, MARKER_TEXTURE_SIZE, type.getAccentColor());
	}

	private void extractCard(final GuiGraphicsExtractor graphics, final int width, final int height) {
		graphics.fill(this.cardX, this.cardY, this.cardX + width, this.cardY + height, CARD_EDGE);
		graphics.fill(this.cardX + 1, this.cardY + 1, this.cardX + width - 1, this.cardY + height - 1, CARD_FILL);
	}

	private void extractPlaceCard(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		this.extractCard(graphics, CARD_WIDTH, CARD_HEIGHT);
		graphics.fill(this.cardX + 4, this.cardY + 16, this.cardX + CARD_WIDTH - 4, this.cardY + 17, HINT_COLOR);
		this.label.extractRenderState(graphics, mouseX, mouseY, a);
		if (this.label.getValue().isEmpty()) {
			graphics.text(Minecraft.getInstance().font, Component.translatable("handbook.atlas.label"), this.cardX + 5, this.cardY + 5, HINT_COLOR, false);
		}

		AtlasMarker.Type[] types = AtlasMarker.Type.values();
		for (int i = 0; i < types.length; i++) {
			int x = this.cardX + 4 + i % COLUMNS * CELL;
			int y = this.cardY + 20 + i / COLUMNS * CELL;
			boolean hovered = mouseX >= x && mouseY >= y && mouseX < x + CELL && mouseY < y + CELL;
			if (hovered) {
				graphics.fill(x, y, x + CELL, y + CELL, HIGHLIGHT);
				graphics.setTooltipForNextFrame(Minecraft.getInstance().font, Component.translatable("handbook.marker." + types[i].getSerializedName()), mouseX, mouseY);
			}

			extractIcon(graphics, types[i], x, y);
		}
	}

	private void extractRemoveCard(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
		this.extractCard(graphics, REMOVE_WIDTH, REMOVE_HEIGHT);
		if (mouseX >= this.cardX && mouseY >= this.cardY && mouseX < this.cardX + REMOVE_WIDTH && mouseY < this.cardY + REMOVE_HEIGHT) {
			graphics.fill(this.cardX + 1, this.cardY + 1, this.cardX + REMOVE_WIDTH - 1, this.cardY + REMOVE_HEIGHT - 1, HIGHLIGHT);
		}

		graphics.blit(RenderPipelines.GUI_TEXTURED, REMOVE_LOCATION, this.cardX + 3, this.cardY + 3, 0.0F, 0.0F, 16, 16, 16, 16);
		graphics.text(Minecraft.getInstance().font, Component.translatable("handbook.atlas.remove_marker"), this.cardX + 22, this.cardY + 7, RecipesPage.INK_COLOR, false);
	}

	private void openCard(final int x, final int y, final int width, final int height) {
		this.cardX = Mth.clamp(x, this.getX(), this.getX() + this.width - width);
		this.cardY = Mth.clamp(y, this.getY(), this.getY() + this.height - height);
	}

	private void closeCard() {
		this.label = null;
		this.removing = null;
	}

	private @Nullable AtlasMarker markerAt(final double mouseX, final double mouseY) {
		double scale = this.blocksPerPixel();
		double leftBlock = this.centerX - this.width / 2.0 * scale;
		double topBlock = this.centerZ - this.height / 2.0 * scale;
		for (AtlasMarker marker : ClientAtlas.getMarkers().reversed()) {
			double x = this.getX() + (marker.x() + 0.5 - leftBlock) / scale - marker.type().getAnchorX() + MARKER_PADDING;
			double y = this.getY() + (marker.z() + 0.5 - topBlock) / scale - marker.type().getAnchorY() + MARKER_PADDING;
			if (mouseX >= x && mouseY >= y && mouseX < x + MARKER_SIZE && mouseY < y + MARKER_SIZE) {
				return marker;
			}
		}

		return null;
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		if (!this.isMouseOver(event.x(), event.y())) {
			this.closeCard();
			return false;
		}

		if (this.label != null && this.clickPlaceCard(event)) {
			return true;
		}

		if (this.removing != null && event.x() >= this.cardX && event.y() >= this.cardY && event.x() < this.cardX + REMOVE_WIDTH && event.y() < this.cardY + REMOVE_HEIGHT) {
			ClientPlayNetworking.send(new RemoveAtlasMarkerPayload(this.removing.id()));
			this.closeCard();
			return true;
		}

		this.closeCard();
		if (event.button() != InputConstants.MOUSE_BUTTON_RIGHT) {
			return super.mouseClicked(event, doubleClick);
		}

		AtlasMarker marker = this.markerAt(event.x(), event.y());
		if (marker != null) {
			this.removing = marker;
			this.openCard((int)event.x(), (int)event.y(), REMOVE_WIDTH, REMOVE_HEIGHT);
			return true;
		}

		double scale = this.blocksPerPixel();
		this.markerX = Mth.floor(this.centerX + (event.x() - this.getX() - this.width / 2.0) * scale);
		this.markerZ = Mth.floor(this.centerZ + (event.y() - this.getY() - this.height / 2.0) * scale);
		this.openCard((int)event.x(), (int)event.y(), CARD_WIDTH, CARD_HEIGHT);
		this.label = new EditBox(Minecraft.getInstance().font, this.cardX + 5, this.cardY + 5, CARD_WIDTH - 10, 10, Component.translatable("handbook.atlas.label"));
		this.label.setMaxLength(AtlasMarker.MAX_LABEL_LENGTH);
		this.label.setBordered(false);
		this.label.setTextColor(RecipesPage.INK_COLOR);
		this.label.setTextShadow(false);
		this.label.setFocused(true);
		return true;
	}

	private boolean clickPlaceCard(final MouseButtonEvent event) {
		double x = event.x() - this.cardX - 4;
		double y = event.y() - this.cardY - 20;
		if (event.x() < this.cardX || event.y() < this.cardY || event.x() >= this.cardX + CARD_WIDTH || event.y() >= this.cardY + CARD_HEIGHT) {
			return false;
		}

		AtlasMarker.Type[] types = AtlasMarker.Type.values();
		int index = x >= 0 && y >= 0 && x < COLUMNS * CELL ? (int)(y / CELL) * COLUMNS + (int)(x / CELL) : -1;
		if (index >= 0 && index < types.length) {
			ClientPlayNetworking.send(new PlaceAtlasMarkerPayload(types[index], this.markerX, this.markerZ, this.label.getValue()));
			this.closeCard();
		}

		return true;
	}

	@Override
	public boolean keyPressed(final KeyEvent event) {
		if (this.label != null && event.key() == InputConstants.KEY_ESCAPE) {
			this.closeCard();
			return true;
		}

		if (this.label != null && event.key() == InputConstants.KEY_RETURN) {
			ClientPlayNetworking.send(new PlaceAtlasMarkerPayload(AtlasMarker.Type.POINT, this.markerX, this.markerZ, this.label.getValue()));
			this.closeCard();
			return true;
		}

		return this.label != null && this.label.keyPressed(event);
	}

	@Override
	public boolean charTyped(final CharacterEvent event) {
		return this.label != null && this.label.charTyped(event);
	}

	private void extractPlayer(final GuiGraphicsExtractor graphics, final double leftBlock, final double topBlock, final double scale) {
		Player player = Minecraft.getInstance().player;
		float x = (float)(this.getX() + (player.getX() - leftBlock) / scale);
		float y = (float)(this.getY() + (player.getZ() - topBlock) / scale);
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().rotate((float)Math.toRadians(player.getYRot() + 180.0F));
		graphics.blit(RenderPipelines.GUI_TEXTURED, PLAYER_LOCATION, -4, -4, 0.0F, 0.0F, 7, 8, 7, 8);
		graphics.pose().popMatrix();
	}

	@Override
	protected void onDrag(final MouseButtonEvent event, final double dx, final double dy) {
		if (this.label != null || this.removing != null) {
			return;
		}

		this.centerX -= dx * this.blocksPerPixel();
		this.centerZ -= dy * this.blocksPerPixel();
	}

	@Override
	public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
		int level = 0;
		while (level < ZOOM_LEVELS.length - 1 && ZOOM_LEVELS[level] < subtile) {
			level++;
		}

		subtile = ZOOM_LEVELS[Mth.clamp(level + (scrollY > 0 ? 1 : scrollY < 0 ? -1 : 0), 0, ZOOM_LEVELS.length - 1)];

		return true;
	}

	@Override
	protected void updateWidgetNarration(final NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}

	private record RasterKey(
		String dimension, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ, int originX, int originY, int subtile, int atlasVersion, int sheenVersion, int highlighted
	) {
	}
}
