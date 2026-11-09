package io.github.schluid.hardmodeplus.mixin;

import io.github.schluid.hardmodeplus.feature.Stamina;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerAttackMixin {
	@Inject(method = "attack", at = @At("HEAD"))
	private void hardmodeplus$attackCost(Entity target, CallbackInfo info) {
		if ((Object) this instanceof ServerPlayer player && target.isAttackable()) {
			Stamina.attacked(player, player.getAttackStrengthScale(0.5F));
		}
	}
}
