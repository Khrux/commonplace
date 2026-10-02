package com.khrux.handbook.client.gui.screens;

import com.khrux.handbook.Handbook;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public enum HandbookTab {
	RECIPES("recipes"),
	FIELD_GUIDE("field_guide"),
	ATLAS("atlas"),
	NOTES("notes");

	private final String serializedName;
	private final Component name;
	private final Identifier iconTexture;

	HandbookTab(final String name) {
		this.serializedName = name;
		this.name = Component.translatable("handbook.tab." + name);
		this.iconTexture = Handbook.id("textures/gui/icon/" + name + ".png");
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
		return this != FIELD_GUIDE || FabricLoader.getInstance().isModLoaded("fieldguide");
	}
}
