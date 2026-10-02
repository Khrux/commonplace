package com.khrux.handbook.client.gui.components;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.client.atlas.AtlasTextures;
import com.khrux.handbook.client.atlas.AtlasTileRenderer;
import com.khrux.handbook.client.atlas.ClientAtlas;
import com.khrux.handbook.client.gui.screens.RecipesPage;
import com.khrux.handbook.network.protocol.PlaceAtlasMarkerPayload;
import com.khrux.handbook.network.protocol.RemoveAtlasMarkerPayload;
import com.khrux.handbook.world.level.atlas.AtlasMarker;
import com.mojang.blaze3d.platform.InputConstants;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public class AtlasMap extends AbstractWidget {
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
		AtlasTileRenderer.render(
			(texture, x, y, size, u0, u1, v0, v1) -> graphics.blit(texture, x, y, x + size, y + size, u0, u1, v0, v1),
			minChunkX,
			minChunkZ,
			maxChunkX,
			maxChunkZ,
			originX,
			originY,
			subtile
		);
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
}
