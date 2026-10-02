package com.khrux.handbook.client;

import com.khrux.handbook.Handbook;
import com.khrux.handbook.client.gui.screens.HandbookTab;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

public class HandbookKeyMappings {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Handbook.id("handbook"));
	public static final KeyMapping SPYGLASS_ZOOM = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.handbook.spyglass_zoom", InputConstants.KEY_Z, CATEGORY));
	private static final Map<HandbookTab, KeyMapping> TAB_KEYS = new EnumMap<>(HandbookTab.class);

	static {
		for (HandbookTab tab : HandbookTab.values()) {
			TAB_KEYS.put(tab, KeyMappingHelper.registerKeyMapping(new KeyMapping("key.handbook.open_" + tab.getSerializedName(), InputConstants.UNKNOWN.getValue(), CATEGORY)));
		}
	}

	public static KeyMapping getTabKey(final HandbookTab tab) {
		return TAB_KEYS.get(tab);
	}

	public static void bootstrap() {
	}
}
