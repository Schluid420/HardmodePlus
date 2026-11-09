package io.github.schluid.hardmodeplus.mobs;

import io.github.schluid.hardmodeplus.HardmodePlus;
import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardArea;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import io.github.schluid.hardmodeplus.config.MobStat;
import io.github.schluid.hardmodeplus.feature.Features;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

// Puts the configured values on a mob as attribute modifiers
public final class MobApplier {
	public static final Identifier MOB_VALUE = Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "mob_value");
	public static final Identifier HARD_MODE = Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "hard_mode");
	public static final Identifier SIEGE = Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "siege");
	private static final double EPSILON = 1.0E-6;

	private MobApplier() {
	}

	public static void applyAll(MinecraftServer server) {
		int count = 0;
		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (apply(entity, level)) {
					count++;
				}
			}
		}
		HardmodePlus.LOGGER.debug("Updated {} mobs", count);
	}

	public static boolean apply(Entity entity, ServerLevel level) {
		if (!(entity instanceof LivingEntity living) || entity instanceof Player) {
			return false;
		}
		MobList.Entry entry = MobList.find(entity.getType());
		if (entry == null) {
			return false;
		}

		boolean perMob = HardmodeConfig.perMobActive();
		boolean hard = HardmodeConfig.hardModeActive() && level.getDifficulty() == Difficulty.HARD
				&& entry.kind().hasMultiplier()
				&& (entry.kind() != MobList.Kind.BOSS || HardmodeConfig.bossesInHardMode());

		float oldMax = living.getMaxHealth();
		float oldHealth = living.getHealth();

		// 1. Values per mob type
		for (MobStat stat : MobStat.values()) {
			AttributeInstance instance = living.getAttribute(stat.attribute);
			if (instance == null) {
				continue;
			}
			Double wanted = perMob ? HardmodeConfig.getOverride(entry.id(), stat) : null;
			double amount = wanted == null ? 0 : (wanted - MobList.vanilla(entry, stat)) * Growth.factor();
			setModifier(instance, MOB_VALUE, amount, AttributeModifier.Operation.ADD_VALUE);
		}

		// 2. Hard mode percentages
		setHard(living, Attributes.MAX_HEALTH, hard ? hardPercent(HardArea.HEALTH) : 0);
		double speed = hard ? hardPercent(HardArea.SPEED) : 0;
		setHard(living, Attributes.MOVEMENT_SPEED, speed);
		setHard(living, Attributes.FLYING_SPEED, speed);
		setHard(living, Attributes.FOLLOW_RANGE, hard ? hardPercent(HardArea.FOLLOW_RANGE) : 0);

		applySiege(living, entry);

		float newMax = living.getMaxHealth();
		if (Math.abs(newMax - oldMax) > EPSILON && oldMax > 0 && oldHealth > 0) {
			living.setHealth(Math.min(newMax, oldHealth * newMax / oldMax));
		}
		return true;
	}

	private static void applySiege(LivingEntity living, MobList.Entry entry) {
		AttributeInstance range = living.getAttribute(Attributes.FOLLOW_RANGE);
		if (range == null) {
			return;
		}
		double amount = 0;
		if (Features.siegeActive() && entry.kind() == MobList.Kind.HOSTILE) {
			if (range.getModifier(SIEGE) != null) {
				range.removeModifier(SIEGE);
			}
			amount = Math.max(0, HardmodeConfig.get(FeatureSetting.SIEGE_RANGE) - range.getValue());
		}
		// Not saved with the mob: the siege ends with the night anyway
		AttributeModifier current = range.getModifier(SIEGE);
		if (amount < EPSILON) {
			if (current != null) {
				range.removeModifier(SIEGE);
			}
			return;
		}
		range.addOrUpdateTransientModifier(new AttributeModifier(SIEGE, amount, AttributeModifier.Operation.ADD_VALUE));
	}

	private static void setHard(LivingEntity living, Holder<Attribute> attribute, double percent) {
		AttributeInstance instance = living.getAttribute(attribute);
		if (instance != null) {
			setModifier(instance, HARD_MODE, percent / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		}
	}

	private static void setModifier(AttributeInstance instance, Identifier id, double amount,
			AttributeModifier.Operation operation) {
		AttributeModifier current = instance.getModifier(id);
		if (Math.abs(amount) < EPSILON) {
			if (current != null) {
				instance.removeModifier(id);
			}
			return;
		}
		if (current != null && current.operation() == operation && Math.abs(current.amount() - amount) < EPSILON) {
			return;
		}
		instance.addOrReplacePermanentModifier(new AttributeModifier(id, amount, operation));
	}

	public static float damageFactor(Entity attacker, ServerLevel level) {
		if (!HardmodeConfig.hardModeActive() || level.getDifficulty() != Difficulty.HARD
				|| !(attacker instanceof LivingEntity) || attacker instanceof Player) {
			return 1.0F;
		}
		MobList.Entry entry = MobList.find(attacker.getType());
		if (entry == null || !entry.kind().hasMultiplier()
				|| (entry.kind() == MobList.Kind.BOSS && !HardmodeConfig.bossesInHardMode())) {
			return 1.0F;
		}
		double percent = hardPercent(HardArea.DAMAGE);
		return (float) Math.max(0.05, 1.0 + percent / 100.0);
	}

	private static double hardPercent(HardArea area) {
		return HardmodeConfig.hardBonusPercent(area) * Growth.factor() + Growth.surcharge();
	}
}
