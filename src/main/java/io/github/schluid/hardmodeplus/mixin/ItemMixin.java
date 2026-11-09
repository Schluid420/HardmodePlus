package io.github.schluid.hardmodeplus.mixin;

import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import io.github.schluid.hardmodeplus.item.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemMixin {
	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void hardmodeplus$fillBowl(Level level, Player player, InteractionHand hand,
			CallbackInfoReturnable<InteractionResult> info) {
		ItemStack stack = player.getItemInHand(hand);
		if (!stack.is(Items.BOWL) || !HardmodeConfig.isOn(FeatureSetting.THIRST)) {
			return;
		}
		HitResult aim = player.pick(player.blockInteractionRange(), 1.0F, true);
		if (!(aim instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
				|| !level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
			return;
		}
		level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL,
				SoundSource.NEUTRAL, 1.0F, 1.0F);
		info.setReturnValue(InteractionResult.SUCCESS.heldItemTransformedTo(
				ItemUtils.createFilledResult(stack, player, new ItemStack(ModItems.WATER_BOWL))));
	}
}
