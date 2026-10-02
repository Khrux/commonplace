package com.khrux.handbook.client.renderer;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.client.gui.screens.HandbookTab;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class PageSnapshot extends DynamicTexture {
	public static final int LEFT = 10;
	public static final int TOP = 18;
	public static final int WIDTH = 281;
	public static final int HEIGHT = 168;
	private static final Map<HandbookTab, Identifier> SNAPSHOTS = new EnumMap<>(HandbookTab.class);

	private PageSnapshot(final Identifier id, final int width, final int height) {
		super(id::toString, width, height, true);
		this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
	}

	public static @Nullable Identifier get(final HandbookTab tab) {
		return SNAPSHOTS.get(tab);
	}

	public static void capture(final HandbookTab tab, final int bookLeft, final int bookTop) {
		Minecraft minecraft = Minecraft.getInstance();
		RenderTarget target = minecraft.gameRenderer.mainRenderTarget();
		int scale = minecraft.getWindow().getGuiScale();
		int x = (bookLeft + LEFT) * scale;
		int y = target.height - (bookTop + TOP + HEIGHT) * scale;
		int width = WIDTH * scale;
		int height = HEIGHT * scale;
		if (x < 0 || y < 0 || x + width > target.width || y + height > target.height) {
			return;
		}

		Identifier id = Handbook.id("page_snapshot/" + tab.getSerializedName());
		PageSnapshot snapshot = new PageSnapshot(id, width, height);
		RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(target.getColorTexture(), snapshot.getTexture(), 0, 0, 0, x, y, width, height);
		minecraft.getTextureManager().register(id, snapshot);
		SNAPSHOTS.put(tab, id);
	}
}
