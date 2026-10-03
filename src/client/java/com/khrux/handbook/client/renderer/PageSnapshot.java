package com.khrux.handbook.client.renderer;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.HandbookConfig;
import com.khrux.handbook.client.gui.components.EnderInk;
import com.khrux.handbook.client.gui.screens.HandbookTab;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
	private static final double AWAY = -10000.0;
	private static final Identifier ENDER_TEAL = Handbook.id("page_snapshot/ender_teal");
	private static PageSnapshot.@Nullable Leaving leaving;
	private static @Nullable HandbookTab captured;
	private static boolean cleanFrame;
	private static boolean enderTeal;

	private PageSnapshot(final Identifier id, final int width, final int height) {
		super(id::toString, width, height, true);
		this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
	}

	public static void bootstrap() {
		ClientTickEvents.START_CLIENT_TICK.register(PageSnapshot::tick);
	}

	public static void leave(final HandbookTab tab, final int bookLeft, final int bookTop, final Runnable action) {
		if (leaving != null) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		boolean shimmer = tab == HandbookTab.ENDER && HandbookConfig.get().enderPageMotion;
		leaving = new PageSnapshot.Leaving(tab, bookLeft, bookTop, action, shimmer);
		cleanFrame = false;
		if (shimmer) {
			EnderInk.forceTeal(0.0F);
		}

		minecraft.mouseHandler.onMove(minecraft.getWindow().handle(), AWAY, AWAY, 0.0, 0.0);
	}

	public static void frameExtracted() {
		if (leaving != null) {
			cleanFrame = true;
		}
	}

	private static void tick(final Minecraft minecraft) {
		PageSnapshot.Leaving current = leaving;
		if (current == null || !cleanFrame) {
			return;
		}

		if (current.shimmer()) {
			Identifier id = Handbook.id("page_snapshot/" + current.tab().getSerializedName());
			if (copy(id, current.bookLeft(), current.bookTop())) {
				SNAPSHOTS.put(current.tab(), id);
				enderTeal = false;
				leaving = new PageSnapshot.Leaving(current.tab(), current.bookLeft(), current.bookTop(), current.action(), false);
				cleanFrame = false;
				EnderInk.forceTeal(1.0F);
				return;
			}

			EnderInk.release();
		}

		leaving = null;
		if (EnderInk.isForced()) {
			enderTeal = copy(ENDER_TEAL, current.bookLeft(), current.bookTop());
			EnderInk.release();
		} else {
			capture(current.tab(), current.bookLeft(), current.bookTop());
		}

		captured = current.tab();
		current.action().run();
		captured = null;
		minecraft.mouseHandler.resyncMousePosition();
	}

	public static @Nullable Identifier getEnderTeal() {
		return enderTeal ? ENDER_TEAL : null;
	}

	public static @Nullable Identifier get(final HandbookTab tab) {
		return SNAPSHOTS.get(tab);
	}

	public static void capture(final HandbookTab tab, final int bookLeft, final int bookTop) {
		if (tab == captured) {
			return;
		}

		if (leaving != null && leaving.tab() == tab) {
			leaving = null;
			EnderInk.release();
			Minecraft.getInstance().mouseHandler.resyncMousePosition();
		}

		Identifier id = Handbook.id("page_snapshot/" + tab.getSerializedName());
		if (!copy(id, bookLeft, bookTop)) {
			return;
		}

		SNAPSHOTS.put(tab, id);
		if (tab == HandbookTab.ENDER) {
			enderTeal = false;
		}
	}

	private static boolean copy(final Identifier id, final int bookLeft, final int bookTop) {
		Minecraft minecraft = Minecraft.getInstance();
		RenderTarget target = minecraft.gameRenderer.mainRenderTarget();
		int scale = minecraft.getWindow().getGuiScale();
		int x = (bookLeft + LEFT) * scale;
		int y = target.height - (bookTop + TOP + HEIGHT) * scale;
		int width = WIDTH * scale;
		int height = HEIGHT * scale;
		if (x < 0 || y < 0 || x + width > target.width || y + height > target.height) {
			return false;
		}

		PageSnapshot snapshot = new PageSnapshot(id, width, height);
		RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(target.getColorTexture(), snapshot.getTexture(), 0, 0, 0, x, y, width, height);
		minecraft.getTextureManager().register(id, snapshot);
		return true;
	}

	private record Leaving(HandbookTab tab, int bookLeft, int bookTop, Runnable action, boolean shimmer) {
	}
}
