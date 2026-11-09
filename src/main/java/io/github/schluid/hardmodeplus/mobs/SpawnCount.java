package io.github.schluid.hardmodeplus.mobs;

import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

public final class SpawnCount {
	private static ServerLevel current;

	private SpawnCount() {
	}

	public static void begin(ServerLevel level) {
		current = level;
	}

	public static void end() {
		current = null;
	}

	public static int cap(MobCategory category, int vanilla) {
		if (current == null || category != MobCategory.MONSTER || !HardmodeConfig.isOn(FeatureSetting.MOB_COUNT)) {
			return vanilla;
		}
		double percent;
		if (current.dimension() == Level.OVERWORLD) {
			percent = Math.max(HardmodeConfig.get(FeatureSetting.SPAWN_SURFACE), HardmodeConfig.get(FeatureSetting.SPAWN_CAVES));
		} else if (current.dimension() == Level.NETHER) {
			percent = HardmodeConfig.get(FeatureSetting.SPAWN_NETHER);
		} else if (current.dimension() == Level.END) {
			percent = HardmodeConfig.get(FeatureSetting.SPAWN_END);
		} else {
			return vanilla;
		}
		return (int) Math.round(vanilla * percent / 100.0);
	}

	public static boolean keep(ServerLevel level, Mob mob) {
		if (level.dimension() != Level.OVERWORLD || mob.getType().getCategory() != MobCategory.MONSTER
				|| !HardmodeConfig.isOn(FeatureSetting.MOB_COUNT)) {
			return true;
		}
		double surface = HardmodeConfig.get(FeatureSetting.SPAWN_SURFACE);
		double caves = HardmodeConfig.get(FeatureSetting.SPAWN_CAVES);
		double max = Math.max(surface, caves);
		if (max <= 0) {
			return false;
		}
		boolean sky = level.getHeight(Heightmap.Types.MOTION_BLOCKING, mob.getBlockX(), mob.getBlockZ()) <= mob.getBlockY();
		double share = (sky ? surface : caves) / max;
		return share >= 1 || level.getRandom().nextDouble() < share;
	}
}
