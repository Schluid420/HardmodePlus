package io.github.schluid.hardmodeplus.item;

import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import io.github.schluid.hardmodeplus.feature.Thirst;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class CanteenItem extends Item {
	private static final int BAR_COLOR = 0x3F76E4;
	private static final float DRINK_SECONDS = 1.2F;

	private final FeatureSetting sipsSetting;
	private final FeatureSetting thirst;
	private final FeatureSetting saturation;
	private final String name;

	public CanteenItem(Properties properties) {
		this(properties, FeatureSetting.FLASK_SIPS, FeatureSetting.THIRST_FLASK, FeatureSetting.THIRST_FLASK_SAT, "canteen");
	}

	public CanteenItem(Properties properties, FeatureSetting sipsSetting, FeatureSetting thirst, FeatureSetting saturation,
			String name) {
		super(properties.stacksTo(1).component(ModItems.SIPS, 0)
				.component(DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(DRINK_SECONDS)
						.animation(ItemUseAnimation.DRINK).sound(SoundEvents.GENERIC_DRINK).hasConsumeParticles(false)
						.build()));
		this.sipsSetting = sipsSetting;
		this.thirst = thirst;
		this.saturation = saturation;
		this.name = name;
	}

	public int maxSips() {
		return sipsSetting == null ? 1 : (int) Math.round(HardmodeConfig.get(sipsSetting));
	}

	public int sips(ItemStack stack) {
		return Math.min(maxSips(), stack.getOrDefault(ModItems.SIPS, 0));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
		if (hit.getType() == HitResult.Type.BLOCK && sips(stack) < maxSips()) {
			BlockPos pos = hit.getBlockPos();
			if (level.getFluidState(pos).is(FluidTags.WATER)) {
				stack.set(ModItems.SIPS, maxSips());
				level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL,
						SoundSource.NEUTRAL, 1.0F, 1.0F);
				return InteractionResult.SUCCESS;
			}
		}
		if (sips(stack) <= 0) {
			return InteractionResult.FAIL;
		}
		return super.use(level, player, hand);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		int sips = sips(stack);
		if (sips <= 0) {
			return stack;
		}
		if (entity instanceof ServerPlayer player) {
			Thirst.drink(player, HardmodeConfig.get(thirst), HardmodeConfig.get(saturation));
		}
		stack.set(ModItems.SIPS, sips - 1);
		return stack;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13.0F * sips(stack) / Math.max(1, maxSips()));
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return BAR_COLOR;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.hardmodeplus." + name + ".sips", sips(stack), maxSips())
				.withStyle(ChatFormatting.BLUE));
		Tooltips.add(tooltip, "item.hardmodeplus." + name + ".tip");
	}
}
