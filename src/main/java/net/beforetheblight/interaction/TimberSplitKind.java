package net.beforetheblight.interaction;

import net.minecraft.util.StringRepresentable;

/**
 * Named hand-splitting outcomes which a timber process may optionally expose.
 */
public enum TimberSplitKind implements StringRepresentable {
	SHINGLES("shingles"),
	RAILS("rails");

	private final String serializedName;

	TimberSplitKind(String serializedName) {
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
