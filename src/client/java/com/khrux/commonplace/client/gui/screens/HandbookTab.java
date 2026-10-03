package com.khrux.commonplace.client.gui.screens;

import com.khrux.commonplace.Commonplace;
import com.khrux.commonplace.world.entity.player.CommonplaceAttachmentTypes;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public enum HandbookTab {
	ENDER("ender"),
	RECIPES("recipes"),
	FIELD_GUIDE("field_guide"),
	ATLAS("atlas"),
	NOTES("notes");

	private final String serializedName;
	private final Component name;
	private final Identifier iconTexture;

	HandbookTab(final String name) {
		this.serializedName = name;
		this.name = Component.translatable("commonplace.tab." + name);
		this.iconTexture = Commonplace.id("textures/gui/icon/" + name + ".png");
	}

	public String getSerializedName() {
		return this.serializedName;
	}

	public HandbookTab step(final int direction) {
		HandbookTab[] tabs = values();
		for (int i = this.ordinal() + direction; i >= 0 && i < tabs.length; i += direction) {
			if (tabs[i].isAvailable()) {
				return tabs[i];
			}
		}

		return this;
	}

	public Component getName() {
		return this.name;
	}

	public Identifier getIconTexture() {
		return this.iconTexture;
	}

	public boolean isAvailable() {
		if (this == ENDER) {
			Player player = Minecraft.getInstance().player;
			return player != null && !player.getAttachedOrElse(CommonplaceAttachmentTypes.QUILL_SLOT, ItemStack.EMPTY).isEmpty();
		}

		return this != FIELD_GUIDE || FabricLoader.getInstance().isModLoaded("fieldguide");
	}
}
