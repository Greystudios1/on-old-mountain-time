package net.beforetheblight.interaction;

import net.minecraft.util.StringRepresentable;

/**
 * Stable timber identities stored in bounded processing block states.
 * Serialized names are save data and must not be renamed.
 */
public enum TimberType implements StringRepresentable {
	CHESTNUT("chestnut"),
	OAK("oak"),
	SPRUCE("spruce");

	private final String serializedName;

	TimberType(String serializedName) {
		this.serializedName = serializedName;
	}

	@Override
	public String getSerializedName() {
		return this.serializedName;
	}

	@Override
	public String toString() {
		return this.serializedName;
	}
}
