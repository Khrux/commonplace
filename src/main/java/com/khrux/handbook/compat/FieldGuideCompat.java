package com.khrux.handbook.compat;

import com.evandev.fieldguide.config.ServerConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class FieldGuideCompat {
	public static void bootstrap() {
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			ServerConfig.getLocal().enableCopyingPages = false;
			ServerConfig.getLocal().enableFieldGuideItem = false;
			ServerConfig.getLocal().requireItemToOpen = false;
		});
	}
}
