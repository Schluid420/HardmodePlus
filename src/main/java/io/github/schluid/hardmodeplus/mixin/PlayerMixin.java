package io.github.schluid.hardmodeplus.mixin;

import io.github.schluid.hardmodeplus.feature.Features;
import io.github.schluid.hardmodeplus.mobs.MobApplier;
import io.github.schluid.hardmodeplus.mobs.Variants;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public abstract class PlayerMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float hardmodeplus$scaleDamage(float amount, ServerLevel level, DamageSource source, float original) {
		if (source.getEntity() == null) {
			return amount;
		}
		return amount * MobApplier.damageFactor(source.getEntity(), level) * Variants.damageFactor(source.getEntity());
	}

	@ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
	private float hardmodeplus$scaleExhaustion(float amount) {
		return amount * Features.exhaustionFactor(((Player) (Object) this).isSprinting());
	}
}
