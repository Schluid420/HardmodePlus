package io.github.schluid.hardmodeplus.client;

import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import net.minecraft.world.effect.MobEffectInstance;

public final class BlastShockView {
	private BlastShockView() {
	}

	public static Float strength(MobEffectInstance nausea, float partialTick) {
		if (nausea == null || nausea.isVisible()) {
			return null;
		}
		double total = HardmodeConfig.get(FeatureSetting.BLAST_SECONDS) * 20;
		if (total <= 0) {
			return null;
		}
		return (float) Math.max(0, Math.min(1, (nausea.getDuration() - partialTick) / total));
	}
}
