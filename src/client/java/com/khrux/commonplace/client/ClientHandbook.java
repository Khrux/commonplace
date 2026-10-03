package com.khrux.commonplace.client;

import com.khrux.commonplace.client.atlas.ClientAtlas;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.jspecify.annotations.Nullable;

public class ClientHandbook {
	private static final int SEND_INTERVAL = 2;
	private static @Nullable ClientHandbook current;
	private final ClientAtlas atlas = new ClientAtlas();
	private final ClientNotebook notebook = new ClientNotebook();
	private final ClientPassphrases passphrases = new ClientPassphrases();
	private final ClientSheen sheen = new ClientSheen();
	private int ticks;

	public static void bootstrap() {
		ClientPlayConnectionEvents.DISCONNECT.register((listener, minecraft) -> current = null);
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			if (current != null && ++current.ticks % SEND_INTERVAL == 0) {
				current.notebook.flush();
			}
		});
	}

	public static ClientHandbook get() {
		if (current == null) {
			current = new ClientHandbook();
		}

		return current;
	}

	public ClientAtlas atlas() {
		return this.atlas;
	}

	public ClientNotebook notebook() {
		return this.notebook;
	}

	public ClientPassphrases passphrases() {
		return this.passphrases;
	}

	public ClientSheen sheen() {
		return this.sheen;
	}
}
