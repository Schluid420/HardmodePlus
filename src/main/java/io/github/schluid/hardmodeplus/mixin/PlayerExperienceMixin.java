package io.github.schluid.hardmodeplus.mixin;

import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public abstract class PlayerExperienceMixin {
	@ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
	private int hardmodeplus$scaleExperience(int amount) {
		if (amount <= 0 || !((Object) this instanceof ServerPlayer player) || !HardmodeConfig.isOn(FeatureSetting.XP)) {
			return amount;
		}
		double scaled = amount * (1 + HardmodeConfig.get(FeatureSetting.XP_GAIN) / 100.0);
		int whole = (int) Math.floor(scaled);
		return whole + (player.getRandom().nextDouble() < scaled - whole ? 1 : 0);
	}
}
