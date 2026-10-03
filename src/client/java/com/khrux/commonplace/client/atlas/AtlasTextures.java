package com.khrux.commonplace.client.atlas;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.khrux.commonplace.Commonplace;
import com.khrux.commonplace.world.level.atlas.AtlasSurveyor;
import com.khrux.commonplace.world.level.atlas.WorldAtlas;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public class AtlasTextures {
	private static final String FALLBACK_LAND = "minecraft:plains";
	private static @Nullable AtlasTextures loaded;
	private final Map<String, AtlasTileTexture> textures = new HashMap<>();
	private final Map<String, List<List<AtlasTileTexture>>> providers = new HashMap<>();
	private final Map<String, AtlasTextures.StructureMarker> structureMarkers = new HashMap<>();

	private AtlasTextures(final ResourceManager resourceManager) {
		JsonObject tiles = read(resourceManager, "atlas/tiles.json");
		JsonObject providers = read(resourceManager, "atlas/providers.json");
		JsonObject markers = read(resourceManager, "atlas/structure_markers.json");
		if (tiles == null || providers == null) {
			return;
		}

		if (markers != null) {
			for (Map.Entry<String, JsonElement> entry : markers.entrySet()) {
				JsonObject marker = entry.getValue().getAsJsonObject();
				this.structureMarkers.put(
					entry.getKey(),
					new AtlasTextures.StructureMarker(
						Commonplace.id("textures/atlas/marker/structure/" + entry.getKey() + ".png"),
						marker.get("width").getAsInt(),
						marker.get("height").getAsInt(),
						marker.get("texture_width").getAsInt(),
						marker.get("texture_height").getAsInt(),
						marker.get("offset_x").getAsInt(),
						marker.get("offset_y").getAsInt(),
						marker.get("near_clip").getAsBoolean()
					)
				);
			}
		}

		for (Map.Entry<String, JsonElement> entry : tiles.entrySet()) {
			boolean innerBorder = entry.getValue().getAsJsonObject().get("inner_border").getAsBoolean();
			this.textures.put(entry.getKey(), new AtlasTileTexture(Commonplace.id("textures/atlas/tile/" + entry.getKey() + ".png"), innerBorder));
		}

		for (Map.Entry<String, JsonElement> entry : tiles.entrySet()) {
			JsonObject tiling = entry.getValue().getAsJsonObject();
			AtlasTileTexture texture = this.textures.get(entry.getKey());
			this.link(texture.getTilesTo(), tiling.getAsJsonArray("tiles_to"));
			this.link(texture.getTilesToHorizontal(), tiling.getAsJsonArray("tiles_to_horizontal"));
			this.link(texture.getTilesToVertical(), tiling.getAsJsonArray("tiles_to_vertical"));
		}

		for (Map.Entry<String, JsonElement> entry : providers.entrySet()) {
			List<List<AtlasTileTexture>> elevations = new ArrayList<>();
			for (JsonElement elevation : entry.getValue().getAsJsonArray()) {
				List<AtlasTileTexture> choices = new ArrayList<>();
				for (JsonElement id : elevation.getAsJsonArray()) {
					choices.add(this.textures.get(id.getAsString()));
				}

				elevations.add(choices);
			}

			this.providers.put(entry.getKey(), elevations);
		}
	}

	public static AtlasTextures get(final ResourceManager resourceManager) {
		if (loaded == null) {
			loaded = new AtlasTextures(resourceManager);
		}

		return loaded;
	}

	public static void reload(final ResourceManager resourceManager) {
		loaded = new AtlasTextures(resourceManager);
	}

	private void link(final Set<AtlasTileTexture> target, final JsonArray ids) {
		for (JsonElement id : ids) {
			AtlasTileTexture texture = this.textures.get(id.getAsString());
			if (texture != null) {
				target.add(texture);
			}
		}
	}

	private static @Nullable JsonObject read(final ResourceManager resourceManager, final String path) {
		Optional<Resource> resource = resourceManager.getResource(Commonplace.id(path));
		if (resource.isEmpty()) {
			return null;
		}

		try (Reader reader = resource.get().openAsReader()) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		} catch (IOException e) {
			return null;
		}
	}

	public AtlasTextures.@Nullable StructureMarker getStructureMarker(final String name) {
		return this.structureMarkers.get(name);
	}

	public @Nullable AtlasTileTexture getTexture(final String provider, final int elevation, final int chunkX, final int chunkZ) {
		if (provider.startsWith(AtlasSurveyor.TILE_PREFIX)) {
			return this.textures.get(provider.substring(AtlasSurveyor.TILE_PREFIX.length()));
		}

		int separator = provider.indexOf(AtlasSurveyor.FALLBACK_SEPARATOR);
		String biome = separator < 0 ? provider : provider.substring(0, separator);
		List<List<AtlasTileTexture>> elevations = this.providers.get(biome);
		if (elevations == null && separator >= 0) {
			elevations = this.providers.get(provider.substring(separator + 1));
		}

		if (elevations == null) {
			elevations = this.providers.get(fallback(biome));
		}

		if (elevations == null || elevations.isEmpty()) {
			return null;
		}

		List<AtlasTileTexture> choices = elevations.get(elevation == WorldAtlas.NO_ELEVATION ? 0 : Math.min(elevation, elevations.size() - 1));
		if (choices.isEmpty()) {
			return null;
		}

		int variation = (int)(Mth.getSeed(chunkX, chunkZ, chunkX * chunkZ) & Integer.MAX_VALUE);
		return choices.get(variation % choices.size());
	}

	private static String fallback(final String provider) {
		return provider.contains("ocean") || provider.contains("river") ? AtlasSurveyor.WATER : FALLBACK_LAND;
	}

	public record StructureMarker(
		Identifier texture, int width, int height, int textureWidth, int textureHeight, int offsetX, int offsetY, boolean nearClip
	) {
	}
}
