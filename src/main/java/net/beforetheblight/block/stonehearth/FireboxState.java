package net.beforetheblight.block.stonehearth;

import net.minecraft.util.StringRepresentable;

/**
 * Persistent, blockstate-only fire conditions for the modular fieldstone
 * firebox. No ticking block entity is needed for these four visual states.
 */
public enum FireboxState implements StringRepresentable {
	COLD("cold"),
	ASH("ash"),
	EMBER("ember"),
	ACTIVE("active");

	private final String serializedName;

	FireboxState(String serializedName) {
		this.serializedName = serializedName;
	}

	@Override
	public String getSerializedName() {
		return this.serializedName;
	}
}
