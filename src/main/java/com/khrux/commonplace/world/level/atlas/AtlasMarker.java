package com.khrux.commonplace.world.level.atlas;

import com.khrux.commonplace.Commonplace;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import java.util.function.IntFunction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public record AtlasMarker(UUID id, UUID owner, AtlasMarker.Type type, int x, int z, String label) {
	public static final int MAX_LABEL_LENGTH = 32;
	public static final Codec<AtlasMarker> CODEC = RecordCodecBuilder.create(
		i -> i.group(
				UUIDUtil.CODEC.fieldOf("id").forGetter(AtlasMarker::id),
				UUIDUtil.CODEC.fieldOf("owner").forGetter(AtlasMarker::owner),
				AtlasMarker.Type.CODEC.fieldOf("type").forGetter(AtlasMarker::type),
				Codec.INT.fieldOf("x").forGetter(AtlasMarker::x),
				Codec.INT.fieldOf("z").forGetter(AtlasMarker::z),
				Codec.string(0, MAX_LABEL_LENGTH).optionalFieldOf("label", "").forGetter(AtlasMarker::label)
			)
			.apply(i, AtlasMarker::new)
	);
	public static final StreamCodec<ByteBuf, AtlasMarker> STREAM_CODEC = StreamCodec.composite(
		UUIDUtil.STREAM_CODEC,
		AtlasMarker::id,
		UUIDUtil.STREAM_CODEC,
		AtlasMarker::owner,
		AtlasMarker.Type.STREAM_CODEC,
		AtlasMarker::type,
		ByteBufCodecs.VAR_INT,
		AtlasMarker::x,
		ByteBufCodecs.VAR_INT,
		AtlasMarker::z,
		ByteBufCodecs.stringUtf8(MAX_LABEL_LENGTH),
		AtlasMarker::label,
		AtlasMarker::new
	);

	public enum Type implements StringRepresentable {
		POINT("point", 16, 16, -1),
		BED("bed", 16, 16, -1),
		TOWER("tower", 16, 16, -1),
		PICKAXE("pickaxe", 16, 16, -1),
		DIAMOND("diamond", 16, 16, -1),
		BRUSH("brush", 16, 16, -1),
		SCROLL("scroll", 16, 16, -1),
		SWORD("sword", 16, 16, -1),
		STUCK_SWORD("stuck_sword", 17, 19, -1),
		SKULL("skull", 16, 16, -1),
		RED_X_SMALL("red_x_small", 16, 16, 0xFFB0302A),
		RED_X_LARGE("red_x_large", 16, 16, 0xFFB0302A);

		public static final Codec<AtlasMarker.Type> CODEC = StringRepresentable.fromEnum(AtlasMarker.Type::values);
		private static final IntFunction<AtlasMarker.Type> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
		public static final StreamCodec<ByteBuf, AtlasMarker.Type> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
		private final String name;
		private final int anchorX;
		private final int anchorY;
		private final int accentColor;
		private final Identifier texture;
		private final Identifier accentTexture;

		Type(final String name, final int anchorX, final int anchorY, final int accentColor) {
			this.name = name;
			this.anchorX = anchorX;
			this.anchorY = anchorY;
			this.accentColor = accentColor;
			this.texture = Commonplace.id("textures/atlas/marker/" + name + ".png");
			this.accentTexture = Commonplace.id("textures/atlas/marker/" + name + "_accent.png");
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}

		public int getAnchorX() {
			return this.anchorX;
		}

		public int getAnchorY() {
			return this.anchorY;
		}

		public int getAccentColor() {
			return this.accentColor;
		}

		public Identifier getTexture() {
			return this.texture;
		}

		public Identifier getAccentTexture() {
			return this.accentTexture;
		}
	}
}
