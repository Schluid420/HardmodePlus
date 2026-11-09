package io.github.schluid.hardmodeplus.mixin;

import io.github.schluid.hardmodeplus.feature.Stamina;
import io.github.schluid.hardmodeplus.feature.Torches;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	@Inject(method = "place", at = @At("RETURN"))
	private void hardmodeplus$blockPlaced(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> info) {
		if (!(context.getPlayer() instanceof ServerPlayer player) || !info.getReturnValue().consumesAction()) {
			return;
		}
		Stamina.blockPlaced(player);
		if (context.getLevel() instanceof ServerLevel level) {
			Torches.placed(level, context.getClickedPos());
		}
	}
}
