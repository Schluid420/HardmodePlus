package io.github.schluid.hardmodeplus.client.mixin;

import io.github.schluid.hardmodeplus.client.BlastShockView;
import io.github.schluid.hardmodeplus.feature.Stamina;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "getEffectBlendFactor", at = @At("RETURN"), cancellable = true)
	private void hardmodeplus$blastShockNausea(Holder<MobEffect> effect, float partialTick,
			CallbackInfoReturnable<Float> cir) {
		if (effect != MobEffects.NAUSEA || !((Object) this instanceof LocalPlayer player)) {
			return;
		}
		Float strength = BlastShockView.strength(player.getEffect(MobEffects.NAUSEA), partialTick);
		if (strength != null) {
			cir.setReturnValue(strength);
		}
	}

	@Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
	private void hardmodeplus$jumpNeedsStamina(CallbackInfo ci) {
		if ((Object) this instanceof LocalPlayer player && !Stamina.canAfford(player, Stamina.jumpCost())) {
			ci.cancel();
		}
	}
}
