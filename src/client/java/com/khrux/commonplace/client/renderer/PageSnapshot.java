package com.khrux.commonplace.client.renderer;

import com.khrux.commonplace.Commonplace;
import com.khrux.commonplace.CommonplaceConfig;
import com.khrux.commonplace.client.gui.components.EnderInk;
import com.khrux.commonplace.client.gui.screens.HandbookTab;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
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
	private static final Identifier ENDER_TEAL = Commonplace.id("page_snapshot/ender_teal");
	private static PageSnapshot.@Nullable Leaving leaving;
	private static @Nullable HandbookTab captured;
	private static boolean cleanFrame;
	private static boolean enderTeal;

	private PageSnapshot(final Identifier id, final NativeImage image) {
		super(id::toString, image);
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
		boolean shimmer = tab == HandbookTab.ENDER && CommonplaceConfig.get().enderPageMotion;
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
			Identifier id = Commonplace.id("page_snapshot/" + current.tab().getSerializedName());
			if (copy(id, current.bookLeft(), current.bookTop(), () -> {
				SNAPSHOTS.put(current.tab(), id);
				enderTeal = false;
			})) {
				leaving = new PageSnapshot.Leaving(current.tab(), current.bookLeft(), current.bookTop(), current.action(), false);
				cleanFrame = false;
				EnderInk.forceTeal(1.0F);
				return;
			}

			EnderInk.release();
		}

		leaving = null;
		if (EnderInk.isForced()) {
			copy(ENDER_TEAL, current.bookLeft(), current.bookTop(), () -> enderTeal = true);
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

		Identifier id = Commonplace.id("page_snapshot/" + tab.getSerializedName());
		copy(id, bookLeft, bookTop, () -> {
			SNAPSHOTS.put(tab, id);
			if (tab == HandbookTab.ENDER) {
				enderTeal = false;
			}
		});
	}

	private static boolean copy(final Identifier id, final int bookLeft, final int bookTop, final Runnable copied) {
		Minecraft minecraft = Minecraft.getInstance();
		RenderTarget target = minecraft.gameRenderer.mainRenderTarget();
		int scale = minecraft.getWindow().getGuiScale();
		int x = (bookLeft + LEFT) * scale;
		int y = (bookTop + TOP) * scale;
		int width = WIDTH * scale;
		int height = HEIGHT * scale;
		if (x < 0 || y < 0 || x + width > target.width || y + height > target.height) {
			return false;
		}

		Screenshot.takeScreenshot(target, screen -> {
			try (screen) {
				NativeImage image = new NativeImage(width, height, false);
				screen.copyRect(image, x, y, 0, 0, width, height, false, false);
				minecraft.getTextureManager().register(id, new PageSnapshot(id, image));
				copied.run();
			}
		});
		return true;
	}

	private record Leaving(HandbookTab tab, int bookLeft, int bookTop, Runnable action, boolean shimmer) {
	}
}
