package net.beforetheblight.client.season;

import java.util.List;

import net.beforetheblight.compat.seasons.SeasonalPlantClock;
import net.beforetheblight.registry.ModBlocks;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client-only temperate phenology colors for the optional Serene Seasons
 * profile. Items and every unsupported context deliberately remain opaque
 * white so the authored albedo and its LabPBR companions are unchanged.
 */
public final class SeasonalVisuals {
	public static final int OPAQUE_WHITE = 0xFFFFFFFF;

	/*
	 * The targets describe biology; the strengths keep these multipliers modest
	 * because each texture already contains a finished green/brown palette.
	 * Order is early/mid/late spring, summer, autumn, then winter.
	 */
	private static final SeasonalPalette CHESTNUT = SeasonalPalette.create(
		new int[] {
			0xFF718866, 0xFF8DB875, 0xFFB8D49A,
			0xFFD4E1BA, 0xFFFFFFFF, 0xFFD7C96F,
			0xFFE0B44E, 0xFFC17A3E, 0xFF8F573B,
			0xFF715D4D, 0xFF655D56, 0xFF6B6259
		},
		new double[] {
			0.38, 0.32, 0.22,
			0.14, 0.00, 0.28,
			0.50, 0.62, 0.68,
			0.60, 0.55, 0.48
		},
		0.00
	);

	/*
	 * American beech retains copper-brown marcescent foliage much farther into
	 * dormancy than chestnut, then changes rapidly around spring budbreak.
	 */
	private static final SeasonalPalette AMERICAN_BEECH = SeasonalPalette.create(
		new int[] {
			0xFF7F6048, 0xFFA8C780, 0xFFC6DDA2,
			0xFFDCE6C3, 0xFFFFFFFF, 0xFFE1D697,
			0xFFE9C658, 0xFFD19750, 0xFFA66243,
			0xFF8B624B, 0xFF765C4D, 0xFF7A5D49
		},
		new double[] {
			0.62, 0.28, 0.20,
			0.12, 0.00, 0.22,
			0.48, 0.60, 0.68,
			0.65, 0.62, 0.62
		},
		0.00
	);

	/*
	 * Black walnut holds a full summer green, then turns clear yellow before
	 * dropping its leaves rather than retaining the beech's winter copper.
	 */
	private static final SeasonalPalette BLACK_WALNUT = SeasonalPalette.create(
		new int[] {
			0xFF78634E, 0xFF96BB73, 0xFFBED994,
			0xFFD8E5BA, 0xFFFFFFFF, 0xFFE2D98D,
			0xFFE9C84F, 0xFFD79C42, 0xFF9A633D,
			0xFF75604E, 0xFF675B52, 0xFF6D6055
		},
		new double[] {
			0.60, 0.30, 0.20,
			0.12, 0.00, 0.20,
			0.52, 0.64, 0.70,
			0.66, 0.62, 0.58
		},
		0.00
	);

	private static final SeasonalPalette LOWBUSH_BLUEBERRY = SeasonalPalette.create(
		new int[] {
			0xFF80634F, 0xFFA9C586, 0xFFCCE2AC,
			0xFFE0EAD0, 0xFFFFFFFF, 0xFFE4D8A2,
			0xFFE2A85B, 0xFFC85A4D, 0xFF93453F,
			0xFF765B4B, 0xFF64554A, 0xFF705C4E
		},
		new double[] {
			0.58, 0.30, 0.20,
			0.12, 0.00, 0.22,
			0.50, 0.65, 0.72,
			0.65, 0.62, 0.60
		},
		0.00
	);

	/*
	 * Duff is darkest while damp in spring, stays stable through summer, gains
	 * a brighter fresh-litter interval in early autumn, then dulls in winter.
	 * Its very small brightness variation is derived from the shared stable
	 * position jitter, so neighboring patches vary without flicker.
	 */
	private static final SeasonalPalette FOREST_DUFF = SeasonalPalette.create(
		new int[] {
			0xFF786F66, 0xFF8A7F6E, 0xFFA0927C,
			0xFFE8E1D5, 0xFFFFFFFF, 0xFFF0E7D2,
			0xFFFFFFFF, 0xFFE8B867, 0xFFBC8052,
			0xFF897C6D, 0xFF766F68, 0xFF82786C
		},
		new double[] {
			0.35, 0.30, 0.22,
			0.08, 0.00, 0.08,
			0.00, 0.45, 0.50,
			0.38, 0.42, 0.38
		},
		0.14
	);

	private SeasonalVisuals() {
	}

	public static void initialize() {
		BlockColorRegistry.register(
			List.of(new SeasonalTintSource(CHESTNUT)),
			ModBlocks.CHESTNUT_LEAVES
		);
		BlockColorRegistry.register(
			List.of(new SeasonalTintSource(AMERICAN_BEECH)),
			ModBlocks.AMERICAN_BEECH_LEAVES
		);
		BlockColorRegistry.register(
			List.of(new SeasonalTintSource(BLACK_WALNUT)),
			ModBlocks.BLACK_WALNUT_LEAVES
		);
		BlockColorRegistry.register(
			List.of(new SeasonalTintSource(LOWBUSH_BLUEBERRY)),
			ModBlocks.LOWBUSH_BLUEBERRY
		);
		BlockColorRegistry.register(
			List.of(new SeasonalTintSource(FOREST_DUFF)),
			ModBlocks.FOREST_DUFF
		);
	}

	private record SeasonalTintSource(SeasonalPalette palette) implements BlockTintSource {
		@Override
		public int color(BlockState state) {
			return OPAQUE_WHITE;
		}

		@Override
		public int colorInWorld(
			BlockState state,
			BlockAndTintGetter level,
			BlockPos pos
		) {
			ClientLevel clientLevel = Minecraft.getInstance().level;
			if (clientLevel == null || pos == null) {
				return OPAQUE_WHITE;
			}
			if (!SeasonalPlantClock.seasonalFoliageColorEnabled(clientLevel, pos)) {
				return OPAQUE_WHITE;
			}

			return SeasonalPlantClock.snapshot(clientLevel, pos)
				.map(snapshot -> this.palette.colorAt(
					snapshot.calendarSubseason().ordinal(),
					snapshot.positionJitter()
				))
				.orElse(OPAQUE_WHITE);
		}
	}

	private record SeasonalPalette(int[] multipliers, double positionJitterStrength) {
		private static final int SUBSEASON_COUNT = 12;

		private SeasonalPalette {
			if (multipliers.length != SUBSEASON_COUNT) {
				throw new IllegalArgumentException("A seasonal palette requires 12 subseason colors");
			}
			multipliers = multipliers.clone();
		}

		private static SeasonalPalette create(
			int[] targets,
			double[] strengths,
			double positionJitterStrength
		) {
			if (targets.length != SUBSEASON_COUNT || strengths.length != SUBSEASON_COUNT) {
				throw new IllegalArgumentException("Targets and strengths must each contain 12 values");
			}

			int[] multipliers = new int[SUBSEASON_COUNT];
			for (int index = 0; index < SUBSEASON_COUNT; index++) {
				multipliers[index] = blendFromWhite(targets[index], strengths[index]);
			}
			return new SeasonalPalette(multipliers, positionJitterStrength);
		}

		private int colorAt(int subseason, double positionJitter) {
			int color = this.multipliers[Math.floorMod(subseason, SUBSEASON_COUNT)];
			double boundedJitter = Math.max(-0.25, Math.min(0.25, positionJitter));
			double brightness = 1.0 + (boundedJitter * this.positionJitterStrength);
			return scaleOpaque(color, brightness);
		}

		private static int blendFromWhite(int target, double strength) {
			double boundedStrength = Math.max(0.0, Math.min(1.0, strength));
			return interpolate(OPAQUE_WHITE, target | 0xFF000000, boundedStrength);
		}

		private static int interpolate(int from, int to, double amount) {
			double boundedAmount = Math.max(0.0, Math.min(1.0, amount));
			int red = channelLerp(from >> 16, to >> 16, boundedAmount);
			int green = channelLerp(from >> 8, to >> 8, boundedAmount);
			int blue = channelLerp(from, to, boundedAmount);
			return 0xFF000000 | (red << 16) | (green << 8) | blue;
		}

		private static int channelLerp(int from, int to, double amount) {
			int fromChannel = from & 0xFF;
			int toChannel = to & 0xFF;
			return (int)Math.round(fromChannel + ((toChannel - fromChannel) * amount));
		}

		private static int scaleOpaque(int color, double brightness) {
			int red = scaleChannel(color >> 16, brightness);
			int green = scaleChannel(color >> 8, brightness);
			int blue = scaleChannel(color, brightness);
			return 0xFF000000 | (red << 16) | (green << 8) | blue;
		}

		private static int scaleChannel(int packed, double brightness) {
			int channel = packed & 0xFF;
			return Math.max(0, Math.min(255, (int)Math.round(channel * brightness)));
		}
	}
}
