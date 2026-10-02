package com.khrux.handbook.world.level.atlas;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.khrux.handbook.Handbook;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;

public class AtlasStructures {
	public static final AtlasStructures EMPTY = new AtlasStructures(new JsonObject());
	private final Map<String, AtlasStructures.Entry> pieces = new HashMap<>();
	private final Map<String, AtlasStructures.Entry> pieceTypes = new HashMap<>();
	private final Map<String, AtlasStructures.Entry> starts = new HashMap<>();
	private final Map<String, AtlasStructures.Entry> types = new HashMap<>();
	private final Map<String, AtlasStructures.Entry> tags = new HashMap<>();

	private AtlasStructures(final JsonObject json) {
		read(json, "pieces", this.pieces);
		read(json, "piece_types", this.pieceTypes);
		read(json, "starts", this.starts);
		read(json, "types", this.types);
		read(json, "tags", this.tags);
	}

	public static AtlasStructures load(final ResourceManager resourceManager) {
		Optional<Resource> resource = resourceManager.getResource(Handbook.id("atlas/structures.json"));
		if (resource.isEmpty()) {
			return EMPTY;
		}

		try (Reader reader = resource.get().openAsReader()) {
			return new AtlasStructures(JsonParser.parseReader(reader).getAsJsonObject());
		} catch (IOException e) {
			return EMPTY;
		}
	}

	private static void read(final JsonObject json, final String key, final Map<String, AtlasStructures.Entry> target) {
		if (!json.has(key)) {
			return;
		}

		for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject(key).entrySet()) {
			JsonObject value = entry.getValue().getAsJsonObject();
			List<String> textures = new ArrayList<>();
			if (value.has("textures")) {
				value.getAsJsonArray("textures").forEach(texture -> textures.add(texture.getAsString()));
			}

			int priority = value.has("priority") ? value.get("priority").getAsInt() : 0;
			String marker = value.has("marker") ? value.get("marker").getAsString() : null;
			target.put(entry.getKey(), new AtlasStructures.Entry(List.copyOf(textures), priority, marker));
		}
	}

	public AtlasStructures.@Nullable Entry getPiece(final String template) {
		return this.pieces.get(template);
	}

	public AtlasStructures.@Nullable Entry getPieceType(final String type) {
		return this.pieceTypes.get(type);
	}

	public AtlasStructures.@Nullable Entry getStart(final String structure) {
		return this.starts.get(structure);
	}

	public AtlasStructures.@Nullable Entry getType(final String type) {
		return this.types.get(type);
	}

	public Map<String, AtlasStructures.Entry> getTags() {
		return this.tags;
	}

	public record Entry(List<String> textures, int priority, @Nullable String marker) {
	}
}
