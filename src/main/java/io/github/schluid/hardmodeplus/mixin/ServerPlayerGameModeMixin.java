package io.github.schluid.hardmodeplus.mixin;

import io.github.schluid.hardmodeplus.feature.Stamina;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
	@Shadow
	@Final
	protected ServerPlayer player;

	@Unique
	private float hardmodeplus$hardness;
	@Unique
	private float hardmodeplus$toolSpeed;

	@Inject(method = "destroyBlock", at = @At("HEAD"))
	private void hardmodeplus$rememberHardness(BlockPos pos, CallbackInfoReturnable<Boolean> info) {
		BlockState state = player.level().getBlockState(pos);
		hardmodeplus$hardness = state.getDestroySpeed(player.level(), pos);
		hardmodeplus$toolSpeed = player.getMainHandItem().getDestroySpeed(state);
	}

	@Inject(method = "destroyBlock", at = @At("RETURN"))
	private void hardmodeplus$blockBroken(BlockPos pos, CallbackInfoReturnable<Boolean> info) {
		if (info.getReturnValueZ()) {
			Stamina.blockBroken(player, hardmodeplus$hardness, hardmodeplus$toolSpeed);
		}
	}
}
