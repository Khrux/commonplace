package com.khrux.handbook.client.compat;

import com.khrux.handbook.client.gui.screens.HandbookScreen;
import com.khrux.handbook.world.item.HandbookItem;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

public class FieldGuideBook {
	private static final Identifier FIELD_GUIDE_BOOK = Identifier.fromNamespaceAndPath("fieldguide", "textures/gui/book.png");

	public static void apply(final Minecraft minecraft) {
		NativeImage book = read(minecraft, HandbookScreen.BOOK_LOCATION);
		NativeImage cover = read(minecraft, HandbookScreen.COVER_LOCATION);
		if (book == null || cover == null || book.getWidth() != cover.getWidth() || book.getHeight() != cover.getHeight()) {
			return;
		}

		int dye = HandbookItem.getColor(minecraft.player);
		for (int y = 0; y < book.getHeight(); y++) {
			for (int x = 0; x < book.getWidth(); x++) {
				int mask = cover.getPixel(x, y);
				int alpha = ARGB.alpha(mask);
				if (alpha > 0) {
					book.setPixel(x, y, ARGB.srgbLerp(alpha / 255.0F, book.getPixel(x, y), ARGB.opaque(ARGB.multiply(mask, dye))));
				}
			}
		}

		cover.close();
		minecraft.getTextureManager().register(FIELD_GUIDE_BOOK, new DynamicTexture(FIELD_GUIDE_BOOK::toString, book));
	}

	private static @Nullable NativeImage read(final Minecraft minecraft, final Identifier location) {
		Optional<Resource> resource = minecraft.getResourceManager().getResource(location);
		if (resource.isEmpty()) {
			return null;
		}

		try (InputStream input = resource.get().open()) {
			return NativeImage.read(input);
		} catch (IOException e) {
			return null;
		}
	}
}
