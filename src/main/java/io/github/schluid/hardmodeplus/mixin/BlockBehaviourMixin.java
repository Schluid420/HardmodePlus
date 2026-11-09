package io.github.schluid.hardmodeplus.mixin;

import io.github.schluid.hardmodeplus.feature.Torches;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void hardmodeplus$torchBurnsOut(BlockState state, ServerLevel level, BlockPos pos, RandomSource random,
			CallbackInfo info) {
		if (Torches.burnOut(state, level, pos)) {
			info.cancel();
		}
	}
}
