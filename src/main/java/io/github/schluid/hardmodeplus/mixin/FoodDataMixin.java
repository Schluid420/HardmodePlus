package io.github.schluid.hardmodeplus.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.schluid.hardmodeplus.feature.Features;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
	@WrapOperation(method = "tick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerPlayer;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean hardmodeplus$ownStarvation(ServerPlayer player, ServerLevel level, DamageSource source, float amount,
			Operation<Boolean> original) {
		if (Features.replacesStarvation()) {
			return false;
		}
		return original.call(player, level, source, amount);
	}

	@ModifyArg(method = "tick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V"))
	private float hardmodeplus$healCost(float exhaustion) {
		return exhaustion * Features.healCostFactor();
	}

	@ModifyConstant(method = "tick", constant = @Constant(intValue = 18))
	private int hardmodeplus$healFrom(int vanilla) {
		return Features.healFrom(vanilla);
	}

	@ModifyConstant(method = "tick", constant = @Constant(intValue = 20))
	private int hardmodeplus$fastHealFrom(int vanilla) {
		return Features.fastHealFrom(vanilla);
	}
}
