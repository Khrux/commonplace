package com.khrux.handbook.world.level.atlas;

import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public class AtlasSurveyor {
	public static final String WATER = "handbook:water";
	public static final String SWAMP_WATER = "handbook:swamp_water";
	public static final String ICE = "handbook:ice";
	public static final String LAVA = "handbook:lava";
	public static final String RAVINE = "handbook:ravine";
	public static final String END_VOID = "handbook:end_void";
	public static final String BEDROCK_ROOF = "handbook:bedrock_roof";
	public static final String FALLBACK_SEPARATOR = "|";
	public static final String TILE_PREFIX = "tile:";
	private static final int STEP = 2;
	private static final int NETHER_SCAN_HEIGHT = 50;

	public static int survey(final WorldAtlas atlas, final ServerLevel level, final LevelChunk chunk) {
		Object2IntMap<String> votes = new Object2IntOpenHashMap<>();
		Map<String, Biome> biomes = new HashMap<>();
		int trees = 0;
		int samples = 0;
		int minX = chunk.getPos().getMinBlockX();
		int minZ = chunk.getPos().getMinBlockZ();
		boolean nether = level.dimension() == Level.NETHER;
		for (int x = 0; x < 16; x += STEP) {
			for (int z = 0; z < 16; z += STEP) {
				samples++;
				if (vote(votes, biomes, level, chunk, minX + x, minZ + z, x, z, nether)) {
					trees++;
				}
			}
		}

		String best = null;
		int bestVotes = 0;
		for (Object2IntMap.Entry<String> entry : votes.object2IntEntrySet()) {
			if (entry.getIntValue() > bestVotes) {
				best = entry.getKey();
				bestVotes = entry.getIntValue();
			}
		}

		if (best == null) {
			return atlas.setTile(chunk.getPos().pack(), level.dimension() == Level.END ? END_VOID : BEDROCK_ROOF, WorldAtlas.NO_ELEVATION);
		}

		int split = best.lastIndexOf('#');
		if (split < 0) {
			return atlas.setTile(chunk.getPos().pack(), best, WorldAtlas.NO_ELEVATION);
		}

		String biome = best.substring(0, split);
		String provider = biome + FALLBACK_SEPARATOR + fallback(biomes.get(biome), (float)trees / samples);
		return atlas.setTile(chunk.getPos().pack(), provider, Integer.parseInt(best.substring(split + 1)));
	}

	public static boolean surveyStructures(final WorldAtlas atlas, final AtlasStructures structures, final ServerLevel level, final LevelChunk chunk) {
		ChunkPos pos = chunk.getPos();
		Registry<Structure> registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		AtlasStructures.Entry best = null;
		boolean complete = true;
		for (Map.Entry<Structure, LongSet> reference : chunk.getAllReferences().entrySet()) {
			Structure structure = reference.getKey();
			AtlasStructures.Entry type = structures.getType(String.valueOf(BuiltInRegistries.STRUCTURE_TYPE.getKey(structure.type())));
			for (long start : reference.getValue()) {
				ChunkPos startPos = ChunkPos.unpack(start);
				LevelChunk startChunk = level.getChunkSource().getChunkNow(startPos.x(), startPos.z());
				if (startChunk == null) {
					complete = false;
					continue;
				}

				StructureStart structureStart = startChunk.getStartForStructure(structure);
				if (structureStart == null || !structureStart.isValid()) {
					continue;
				}

				if (start == pos.pack()) {
					String marker = marker(structures, registry, structure, type);
					if (marker != null) {
						atlas.setStructureMarker(start, marker);
					}
				}

				for (StructurePiece piece : structureStart.getPieces()) {
					if (!piece.getBoundingBox().intersects(pos.getMinBlockX(), pos.getMinBlockZ(), pos.getMaxBlockX(), pos.getMaxBlockZ())) {
						continue;
					}

					AtlasStructures.Entry entry = piece(structures, piece);
					if (entry == null) {
						entry = type;
					}

					if (entry != null && !entry.textures().isEmpty() && (best == null || entry.priority() < best.priority())) {
						best = entry;
					}
				}
			}
		}

		if (best != null) {
			int variation = (int)(Mth.getSeed(pos.x(), 0, pos.z()) & Integer.MAX_VALUE);
			atlas.setTile(pos.pack(), TILE_PREFIX + best.textures().get(variation % best.textures().size()), WorldAtlas.NO_ELEVATION);
		}

		return complete;
	}

	private static AtlasStructures.@Nullable Entry piece(final AtlasStructures structures, final StructurePiece piece) {
		if (piece instanceof PoolElementStructurePiece poolPiece && poolPiece.getElement() instanceof SinglePoolElement element) {
			return structures.getPiece(element.getTemplateLocation().toString());
		}

		return structures.getPieceType(String.valueOf(BuiltInRegistries.STRUCTURE_PIECE.getKey(piece.getType())));
	}

	private static @Nullable String marker(
		final AtlasStructures structures, final Registry<Structure> registry, final Structure structure, final AtlasStructures.@Nullable Entry type
	) {
		AtlasStructures.Entry start = structures.getStart(String.valueOf(registry.getKey(structure)));
		if (start != null && start.marker() != null) {
			return start.marker();
		}

		if (type != null && type.marker() != null) {
			return type.marker();
		}

		Holder<Structure> holder = registry.wrapAsHolder(structure);
		for (Map.Entry<String, AtlasStructures.Entry> tag : structures.getTags().entrySet()) {
			if (tag.getValue().marker() != null && holder.is(TagKey.create(Registries.STRUCTURE, Identifier.parse(tag.getKey())))) {
				return tag.getValue().marker();
			}
		}

		return null;
	}

	private static boolean vote(
		final Object2IntMap<String> votes,
		final Map<String, Biome> biomes,
		final ServerLevel level, final LevelChunk chunk, final int x, final int z, final int localX, final int localZ, final boolean nether
	) {
		int surface = nether ? findNetherFloor(chunk, x, z) : chunk.getHeight(Heightmap.Types.WORLD_SURFACE, localX, localZ);
		if (surface <= level.getMinY()) {
			return false;
		}

		int floor = nether ? surface : chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, localX, localZ);
		BlockPos top = new BlockPos(x, surface - 1, z);
		BlockState state = chunk.getBlockState(top);
		Holder<Biome> biome = level.getBiome(top);
		int height = floor - level.getSeaLevel();
		if (state.getFluidState().is(Fluids.WATER) && surface > floor) {
			votes.mergeInt(biome.is(ConventionalBiomeTags.IS_SWAMP) ? SWAMP_WATER : WATER, 4, Integer::sum);
		} else if (state.is(Blocks.ICE)) {
			votes.mergeInt(ICE, 3, Integer::sum);
		} else if (state.getFluidState().is(Fluids.LAVA)) {
			votes.mergeInt(LAVA, 6, Integer::sum);
		} else if (level.dimension() == Level.OVERWORLD && height < -7) {
			votes.mergeInt(RAVINE, 12, Integer::sum);
		}

		String name = biome.getRegisteredNameIfPresent().orElse("minecraft:plains");
		biomes.put(name, biome.value());
		votes.mergeInt(name + "#" + elevation(height), biome.is(BiomeTags.IS_BEACH) ? 3 : biome.is(BiomeTags.IS_NETHER) ? 2 : 1, Integer::sum);
		return state.is(BlockTags.LEAVES);
	}

	private static String fallback(final Biome biome, final float treeCover) {
		boolean trees = treeCover >= 0.1F;
		boolean dense = treeCover >= 0.4F;
		float temperature = biome.getBaseTemperature();
		if (temperature < 0.15F) {
			return trees ? "minecraft:snowy_taiga" : "minecraft:snowy_plains";
		} else if (temperature < 0.5F) {
			return trees ? "minecraft:taiga" : "minecraft:meadow";
		} else if (temperature < 1.0F) {
			return dense ? "minecraft:forest" : "minecraft:plains";
		} else if (biome.hasPrecipitation()) {
			return dense ? "minecraft:jungle" : trees ? "minecraft:sparse_jungle" : "minecraft:plains";
		}

		return trees ? "minecraft:savanna" : "minecraft:desert";
	}

	private static int findNetherFloor(final LevelChunk chunk, final int x, final int z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, NETHER_SCAN_HEIGHT, z);
		for (int y = NETHER_SCAN_HEIGHT; y > chunk.getMinY(); y--) {
			pos.setY(y);
			if (!chunk.getBlockState(pos).isAir()) {
				return y + 1;
			}
		}

		return chunk.getMinY();
	}

	public static int elevation(final int blocksAboveSea) {
		if (blocksAboveSea < 10) {
			return 0;
		} else if (blocksAboveSea < 20) {
			return 1;
		} else if (blocksAboveSea < 35) {
			return 2;
		}

		return blocksAboveSea < 50 ? 3 : 4;
	}
}
