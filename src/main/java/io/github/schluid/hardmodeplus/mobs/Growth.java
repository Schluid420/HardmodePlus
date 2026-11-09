package io.github.schluid.hardmodeplus.mobs;

import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import net.minecraft.server.MinecraftServer;

// Mobs get stronger with the world days (growth up to day X, then a daily surcharge)
public final class Growth {
	private static final double DAY = 24000.0;
	private static final int UPDATES_PER_DAY = 24;

	private static double day;
	private static long lastStep = Long.MIN_VALUE;

	private Growth() {
	}

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) {
			return;
		}
		day = server.overworld().getOverworldClockTime() / DAY;
		boolean active = HardmodeConfig.isOn(FeatureSetting.GROWTH) || HardmodeConfig.isOn(FeatureSetting.SCALING);
		long step = active ? (long) Math.floor(day * UPDATES_PER_DAY) : -1;
		if (step != lastStep) {
			lastStep = step;
			MobApplier.applyAll(server);
			Variants.applyAll(server);
		}
	}

	public static double factor() {
		if (!HardmodeConfig.isOn(FeatureSetting.GROWTH)) {
			return 1;
		}
		return Math.max(0, Math.min(1, day / HardmodeConfig.get(FeatureSetting.GROWTH_DAYS)));
	}

	public static double surcharge() {
		if (!HardmodeConfig.isOn(FeatureSetting.SCALING)) {
			return 0;
		}
		double start = HardmodeConfig.isOn(FeatureSetting.GROWTH) ? HardmodeConfig.get(FeatureSetting.GROWTH_DAYS) : 0;
		double days = Math.max(0, day - start);
		return Math.min(HardmodeConfig.get(FeatureSetting.SCALING_MAX), days * HardmodeConfig.get(FeatureSetting.SCALING_PER_DAY));
	}

	public static void reset() {
		day = 0;
		lastStep = Long.MIN_VALUE;
	}
}
