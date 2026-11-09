package io.github.schluid.hardmodeplus.item;

import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import io.github.schluid.hardmodeplus.effect.ModEffects;
import io.github.schluid.hardmodeplus.feature.Injuries;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.level.Level;

public class TreatmentItem extends Item {
	private final Injuries.Treatment treatment;
	private final FeatureSetting seconds;

	public TreatmentItem(Properties properties, Injuries.Treatment treatment, FeatureSetting seconds) {
		super(properties.stacksTo(16).component(DataComponents.CONSUMABLE, Consumable.builder()
				.consumeSeconds((float) seconds.defaultValue).animation(ItemUseAnimation.BRUSH)
				.sound(treatment == Injuries.Treatment.CAST ? Holder.direct(SoundEvents.BONE_MEAL_USE)
						: Holder.direct(SoundEvents.WOOL_PLACE))
				.hasConsumeParticles(false).build()));
		this.treatment = treatment;
		this.seconds = seconds;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		boolean helps = player instanceof ServerPlayer server ? Injuries.helps(server, treatment) : helpsByIcon(player);
		if (!helps) {
			return InteractionResult.FAIL;
		}
		return super.use(level, player, hand);
	}

	private boolean helpsByIcon(Player player) {
		return switch (treatment) {
			case BANDAGE -> player.hasEffect(ModEffects.BLEEDING);
			case HERBAL -> player.hasEffect(ModEffects.BLEEDING);
			case TOURNIQUET -> player.hasEffect(ModEffects.BLEEDING) || player.hasEffect(ModEffects.HEAVY_BLEEDING);
			case SPLINT -> player.hasEffect(ModEffects.FRACTURE);
			case CAST -> player.hasEffect(ModEffects.FRACTURE) || player.hasEffect(ModEffects.SPLINTED);
		};
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return (int) Math.max(10, Math.round(HardmodeConfig.get(seconds) * 20));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (entity instanceof ServerPlayer player && Injuries.helps(player, treatment)) {
			Injuries.treat(player, treatment);
			stack.consume(1, entity);
		}
		return stack;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		Tooltips.add(tooltip, getDescriptionId() + ".tip");
	}
}
