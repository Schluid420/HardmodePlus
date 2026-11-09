package io.github.schluid.hardmodeplus.client.mixin;

import io.github.schluid.hardmodeplus.client.ClientOptions;
import io.github.schluid.hardmodeplus.effect.ModEffects;
import io.github.schluid.hardmodeplus.feature.Features;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
	@Inject(method = "isSprintingPossible", at = @At("RETURN"), cancellable = true)
	private void hardmodeplus$noSprintWhenExhausted(boolean swimming, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ()) {
			return;
		}
		LocalPlayer player = (LocalPlayer) (Object) this;
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		// Exhausted or broken bone: no sprinting
		if (speed != null && speed.getModifier(Features.EXHAUST_ID) != null || player.hasEffect(ModEffects.FRACTURE)
				|| player.hasEffect(ModEffects.IN_CAST)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "aiStep", at = @At("TAIL"))
	private void hardmodeplus$stopSprintOnRelease(CallbackInfo ci) {
		LocalPlayer player = (LocalPlayer) (Object) this;
		if (!player.isSprinting() || !ClientOptions.sprintFix()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (!minecraft.options.toggleSprint().get() && !minecraft.options.keySprint.isDown()) {
			player.setSprinting(false);
		}
	}
}
