package com.khrux.handbook.client.atlas;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.Identifier;

public class AtlasTileTexture {
	private final Identifier location;
	private final boolean innerBorder;
	private final Set<AtlasTileTexture> tilesTo = new HashSet<>();
	private final Set<AtlasTileTexture> tilesToHorizontal = new HashSet<>();
	private final Set<AtlasTileTexture> tilesToVertical = new HashSet<>();

	public AtlasTileTexture(final Identifier location, final boolean innerBorder) {
		this.location = location;
		this.innerBorder = innerBorder;
	}

	public Identifier getLocation() {
		return this.location;
	}

	public Set<AtlasTileTexture> getTilesTo() {
		return this.tilesTo;
	}

	public Set<AtlasTileTexture> getTilesToHorizontal() {
		return this.tilesToHorizontal;
	}

	public Set<AtlasTileTexture> getTilesToVertical() {
		return this.tilesToVertical;
	}

	public boolean tiles(final AtlasTileTexture other) {
		return this == other || this.innerBorder ^ (this.tilesTo.contains(other) || this.tilesToHorizontal.contains(other) || this.tilesToVertical.contains(other));
	}

	public boolean tilesHorizontally(final AtlasTileTexture other) {
		return this == other || this.innerBorder ^ (this.tilesTo.contains(other) || this.tilesToHorizontal.contains(other));
	}

	public boolean tilesVertically(final AtlasTileTexture other) {
		return this == other || this.innerBorder ^ (this.tilesTo.contains(other) || this.tilesToVertical.contains(other));
	}
}
