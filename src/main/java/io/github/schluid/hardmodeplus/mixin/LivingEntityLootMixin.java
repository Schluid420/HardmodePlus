package io.github.schluid.hardmodeplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.schluid.hardmodeplus.mobs.KillLoot;
import io.github.schluid.hardmodeplus.mobs.Variants;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityLootMixin {
	@ModifyReturnValue(method = "getExperienceReward", at = @At("RETURN"))
	private int hardmodeplus$variantExperience(int xp) {
		return Variants.experience((LivingEntity) (Object) this, xp);
	}

	@WrapOperation(method = "dropAllDeathLoot", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V"))
	private void hardmodeplus$loot(LivingEntity mob, ServerLevel level, DamageSource source, boolean byPlayer,
			Operation<Void> original) {
		if (KillLoot.blocked(mob, source.getEntity())) {
			return;
		}
		original.call(mob, level, source, byPlayer);
		Variants.extraLoot(mob, level, byPlayer, () -> original.call(mob, level, source, byPlayer));
	}

	@Inject(method = "dropExperience", at = @At("HEAD"), cancellable = true)
	private void hardmodeplus$experience(ServerLevel level, Entity killer, CallbackInfo info) {
		if (KillLoot.blocked((LivingEntity) (Object) this, killer)) {
			info.cancel();
		}
	}
}
