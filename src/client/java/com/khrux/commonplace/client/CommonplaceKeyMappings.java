package com.khrux.commonplace.client;

import com.khrux.commonplace.Commonplace;
import com.khrux.commonplace.client.gui.screens.HandbookTab;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

public class CommonplaceKeyMappings {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Commonplace.id("commonplace"));
	public static final KeyMapping SPYGLASS_ZOOM = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.commonplace.spyglass_zoom", InputConstants.KEY_Z, CATEGORY));
	private static final Map<HandbookTab, KeyMapping> TAB_KEYS = new EnumMap<>(HandbookTab.class);

	static {
		for (HandbookTab tab : HandbookTab.values()) {
			TAB_KEYS.put(tab, KeyMappingHelper.registerKeyMapping(new KeyMapping("key.commonplace.open_" + tab.getSerializedName(), InputConstants.UNKNOWN.getValue(), CATEGORY)));
		}
	}

	public static KeyMapping getTabKey(final HandbookTab tab) {
		return TAB_KEYS.get(tab);
	}

	public static void bootstrap() {
	}
}
