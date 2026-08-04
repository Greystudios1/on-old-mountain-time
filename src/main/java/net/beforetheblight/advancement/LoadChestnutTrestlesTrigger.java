package net.beforetheblight.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * Exact server signal for swapping empty sawing trestles to a loaded chestnut
 * state. Unlike a post-state block predicate, this cannot be satisfied by
 * using another beam on a station which was already loaded.
 */
public final class LoadChestnutTrestlesTrigger extends SimpleCriterionTrigger<LoadChestnutTrestlesTrigger.TriggerInstance> {
	public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player")
				.forGetter(TriggerInstance::player)
		).apply(instance, TriggerInstance::new)
	);

	@Override
	public Codec<TriggerInstance> codec() {
		return CODEC;
	}

	public void trigger(ServerPlayer player) {
		this.trigger(player, instance -> true);
	}

	public record TriggerInstance(Optional<ContextAwarePredicate> player)
		implements SimpleCriterionTrigger.SimpleInstance {
	}
}
