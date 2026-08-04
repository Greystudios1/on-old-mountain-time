package net.beforetheblight.block.furniture;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Left/right halves of a two-block-wide furniture object. */
public enum FurniturePart implements StringRepresentable {
	LEFT("left"),
	RIGHT("right");

	public static final Codec<FurniturePart> CODEC =
		StringRepresentable.fromEnum(FurniturePart::values);

	private final String serializedName;

	FurniturePart(String serializedName) {
		this.serializedName = serializedName;
	}

	@Override
	public String getSerializedName() {
		return this.serializedName;
	}

	public FurniturePart opposite() {
		return this == LEFT ? RIGHT : LEFT;
	}
}
