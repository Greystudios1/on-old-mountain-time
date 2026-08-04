package net.beforetheblight.mixin;

import java.lang.ref.WeakReference;

import dev.worldgen.lithostitched.impl.worldgen.biomeinjector.internal.InjectorBiomeSource;
import net.beforetheblight.worldgen.biome.CoveMinimumFootprintFilter;
import net.beforetheblight.worldgen.biome.ModBiomes;
import net.beforetheblight.worldgen.biome.RawInjectorBiomeSourceAccess;
import net.beforetheblight.worldgen.biome.RidgeMinimumFootprintFilter;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InjectorBiomeSource.class)
public abstract class InjectorBiomeSourceMixin implements RawInjectorBiomeSourceAccess {
	@Unique
	private final ThreadLocal<FilterThreadState> beforeTheBlight$filterState =
		ThreadLocal.withInitial(FilterThreadState::new);

	@Shadow
	public abstract BiomeSource directDelegate();

	@Inject(method = "getNoiseBiome", at = @At("RETURN"), cancellable = true)
	private void beforeTheBlight$enforceMinimumBiomeFootprint(
		int quartX,
		int quartY,
		int quartZ,
		Climate.Sampler sampler,
		CallbackInfoReturnable<Holder<Biome>> callback
	) {
		FilterThreadState state = beforeTheBlight$filterState.get();
		if (state.rawSamplingDepth > 0) {
			return;
		}

		Holder<Biome> rawResult = callback.getReturnValue();
		boolean rawRidge = rawResult.is(ModBiomes.CHESTNUT_OAK_RIDGE);
		boolean rawCove = rawResult.is(ModBiomes.HEMLOCK_BEECH_COVE);
		if (!rawRidge && !rawCove) {
			return;
		}

		Object owner = this;
		state.enterScope(owner, sampler);
		boolean keep;
		if (rawRidge) {
			state.classifier.rememberRawRidge(quartX, quartY, quartZ);
			keep = state.classifier.shouldKeep(
				quartX,
				quartY,
				quartZ,
				(sampleX, sampleY, sampleZ) -> beforeTheBlight$getRawNoiseBiome(
					sampleX,
					sampleY,
					sampleZ,
					sampler
				).is(ModBiomes.CHESTNUT_OAK_RIDGE)
			);
		} else {
			state.coveClassifier.rememberRawCove(quartX, quartY, quartZ);
			keep = state.coveClassifier.shouldKeep(
				quartX,
				quartY,
				quartZ,
				(sampleX, sampleY, sampleZ) -> beforeTheBlight$getRawNoiseBiome(
					sampleX,
					sampleY,
					sampleZ,
					sampler
				).is(ModBiomes.HEMLOCK_BEECH_COVE)
			);
		}
		if (!keep) {
			callback.setReturnValue(directDelegate().getNoiseBiome(quartX, quartY, quartZ, sampler));
		}
	}

	@Unique
	@Override
	public Holder<Biome> beforeTheBlight$getRawNoiseBiome(
		int quartX,
		int quartY,
		int quartZ,
		Climate.Sampler sampler
	) {
		FilterThreadState state = beforeTheBlight$filterState.get();
		state.enterScope(this, sampler);
		state.rawSamplingDepth++;
		try {
			return ((InjectorBiomeSource) (Object) this)
				.getNoiseBiome(quartX, quartY, quartZ, sampler);
		} finally {
			state.rawSamplingDepth--;
		}
	}

	@Unique
	private static final class FilterThreadState {
		private final RidgeMinimumFootprintFilter.Classifier classifier =
			new RidgeMinimumFootprintFilter.Classifier();
		private final CoveMinimumFootprintFilter.Classifier coveClassifier =
			new CoveMinimumFootprintFilter.Classifier();
		private WeakReference<Object> owner = new WeakReference<>(null);
		private WeakReference<Climate.Sampler> sampler = new WeakReference<>(null);
		private int rawSamplingDepth;

		private void enterScope(Object nextOwner, Climate.Sampler nextSampler) {
			if (owner.get() != nextOwner || sampler.get() != nextSampler) {
				classifier.clear();
				coveClassifier.clear();
				owner = new WeakReference<>(nextOwner);
				sampler = new WeakReference<>(nextSampler);
			}
		}
	}
}
