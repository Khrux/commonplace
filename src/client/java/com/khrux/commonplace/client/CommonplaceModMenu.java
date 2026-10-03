package com.khrux.commonplace.client;

import com.khrux.commonplace.client.gui.screens.CommonplaceConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class CommonplaceModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return CommonplaceConfigScreen::new;
	}
}
