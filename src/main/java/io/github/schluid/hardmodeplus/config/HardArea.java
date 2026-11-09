package io.github.schluid.hardmodeplus.config;

import java.util.Locale;

public enum HardArea {
	GENERAL("minecraft:nether_star"),
	HEALTH("minecraft:golden_apple"),
	DAMAGE("minecraft:iron_sword"),
	SPEED("minecraft:sugar"),
	FOLLOW_RANGE("minecraft:spyglass");

	public static final double MIN = -90;
	public static final double MAX = 1000;

	public final String iconItem;
	public final String key;

	HardArea(String iconItem) {
		this.iconItem = iconItem;
		this.key = name().toLowerCase(Locale.ROOT);
	}

	public static HardArea byKey(String key) {
		for (HardArea area : values()) {
			if (area.key.equals(key)) {
				return area;
			}
		}
		return null;
	}
}
