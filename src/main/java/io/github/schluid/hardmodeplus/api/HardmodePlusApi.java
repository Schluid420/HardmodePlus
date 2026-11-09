package io.github.schluid.hardmodeplus.api;

import io.github.schluid.hardmodeplus.config.HardArea;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import java.util.Locale;

public final class HardmodePlusApi {
	private HardmodePlusApi() {
	}

	public static boolean isEnabled() {
		return HardmodeConfig.enabled();
	}

	public static double hardBonusPercent(String area) {
		HardArea found = HardArea.byKey(area.toLowerCase(Locale.ROOT));
		return found == null || !HardmodeConfig.hardModeActive() ? 0 : HardmodeConfig.hardBonusPercent(found);
	}
}
