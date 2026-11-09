package io.github.schluid.hardmodeplus.mobs;

import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

public final class KillLoot {
	private KillLoot() {
	}

	public static boolean blocked(LivingEntity mob, Entity killer) {
		if (!(mob instanceof Mob) || mob instanceof EnderDragon || mob instanceof WitherBoss) {
			return false;
		}
		FeatureSetting setting = mob instanceof Enemy ? FeatureSetting.KILL_LOOT_MONSTERS
				: mob instanceof AbstractGolem ? FeatureSetting.KILL_LOOT_GOLEMS : FeatureSetting.KILL_LOOT_ANIMALS;
		return HardmodeConfig.isOn(setting) && !byPlayer(killer);
	}

	private static boolean byPlayer(Entity killer) {
		return killer instanceof Player
				|| killer instanceof OwnableEntity pet && pet.getOwner() instanceof Player;
	}
}
