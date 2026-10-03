package com.khrux.handbook.client;

import com.khrux.handbook.client.gui.screens.HandbookConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class HandbookModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return HandbookConfigScreen::new;
	}
}
