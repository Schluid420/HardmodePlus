package io.github.schluid.hardmodeplus.item;

import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import io.github.schluid.hardmodeplus.feature.Thirst;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class WaterBowlItem extends Item {
	private static final float DRINK_SECONDS = 1.2F;

	public WaterBowlItem(Properties properties) {
		super(properties.stacksTo(1)
				.component(DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(DRINK_SECONDS)
						.animation(ItemUseAnimation.DRINK).sound(SoundEvents.GENERIC_DRINK).hasConsumeParticles(false)
						.build()));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (entity instanceof ServerPlayer player) {
			Thirst.drink(player, HardmodeConfig.get(FeatureSetting.THIRST_BOWL), HardmodeConfig.get(FeatureSetting.THIRST_BOWL_SAT));
		}
		if (entity instanceof Player player && player.getAbilities().instabuild) {
			return stack;
		}
		return new ItemStack(Items.BOWL);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		Tooltips.add(tooltip, "item.hardmodeplus.water_bowl.tip");
	}
}
