package io.github.schluid.hardmodeplus.mixin;

import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import io.github.schluid.hardmodeplus.feature.Thirst;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Consumable.class)
public abstract class ConsumableMixin {
	@Inject(method = "onConsume", at = @At("HEAD"))
	private void hardmodeplus$quenchThirst(Level level, LivingEntity entity, ItemStack stack,
			CallbackInfoReturnable<ItemStack> cir) {
		if (!(entity instanceof ServerPlayer player)) {
			return;
		}
		if (stack.is(Items.POTION)) {
			PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
			if (contents != null && contents.is(Potions.WATER)) {
				Thirst.drink(player, HardmodeConfig.get(FeatureSetting.THIRST_WATER), HardmodeConfig.get(FeatureSetting.THIRST_WATER_SAT));
			}
		} else if (stack.is(Items.MILK_BUCKET)) {
			Thirst.drink(player, HardmodeConfig.get(FeatureSetting.THIRST_MILK), HardmodeConfig.get(FeatureSetting.THIRST_MILK_SAT));
		}
	}
}
