package io.github.schluid.hardmodeplus.config;

import io.github.schluid.hardmodeplus.mobs.MobList;

public enum Presets {
	RELAXED("relaxed"),
	TOUGHER("tougher"),
	VETERAN("veteran"),
	NIGHTMARE("nightmare"),
	REALISM("realism");

	private static final String[] ZOMBIES = {"zombie", "husk", "drowned", "zombie_villager"};
	private static final String[] SKELETONS = {"skeleton", "stray", "bogged", "wither_skeleton"};

	public final String key;

	Presets(String key) {
		this.key = key;
	}

	public void apply() {
		HardmodeConfig.resetAll();
		HardmodeConfig.setEnabled(true);
		HardmodeConfig.setMode(HardmodeConfig.Mode.BOTH);
		HardmodeConfig.setBossesInHardMode(true);
		survival();
		switch (this) {
			case RELAXED -> {
			}
			case TOUGHER -> {
				multiplier(25, 7.5, 37.5);
				monsters(110);
				variants(8);
				experience(-10);
			}
			case VETERAN -> {
				multiplier(50, 15, 75);
				zombies(0.25, 0, 0);
				skeletons(0, 24);
				spiders(0.33);
				monsters(125);
				variants(15);
				experience(-25);
				on(FeatureSetting.KILL_LOOT_MONSTERS);
			}
			case NIGHTMARE -> {
				multiplier(75, 22.5, 112.5);
				zombies(0.26, 2, 0.1);
				skeletons(2, 28);
				spiders(0.34);
				creeper(0.28, 0.25);
				monsters(140);
				variants(23);
				experience(-40);
				on(FeatureSetting.SIEGE);
				on(FeatureSetting.KILL_LOOT_MONSTERS);
				on(FeatureSetting.KILL_LOOT_GOLEMS);
			}
			case REALISM -> {
				multiplier(100, 30, 150);
				zombies(0.27, 4, 0.15);
				skeletons(4, 32);
				spiders(0.35);
				creeper(0.3, 0.5);
				monsters(150);
				variants(30);
				experience(-50);
				on(FeatureSetting.SIEGE);
				on(FeatureSetting.KILL_LOOT_MONSTERS);
				on(FeatureSetting.KILL_LOOT_GOLEMS);
			}
		}
	}

	private static void survival() {
		on(FeatureSetting.DEATH_PENALTY);
		on(FeatureSetting.LOW_HEALTH_DARK);
		on(FeatureSetting.LOW_HEALTH_BLIND);
		HardmodeConfig.set(FeatureSetting.LOW_HEALTH_BLIND_AT, 1);
		on(FeatureSetting.SLEEP_HEAL);
		HardmodeConfig.set(FeatureSetting.SLEEP_FOOD, 6);
		on(FeatureSetting.HEAL);
		HardmodeConfig.set(FeatureSetting.FAST_HEAL, 0);
		on(FeatureSetting.FOOD);
		HardmodeConfig.set(FeatureSetting.STARVE_DELAY, 3);
		HardmodeConfig.set(FeatureSetting.STARVE_INTERVAL, 5);
		on(FeatureSetting.THIRST);
		HardmodeConfig.set(FeatureSetting.THIRST_FIRST_DAY, 75);
		HardmodeConfig.set(FeatureSetting.THIRST_INTERVAL, 5);
		on(FeatureSetting.BACKSTAB);
		HardmodeConfig.set(FeatureSetting.BACKSTAB_SLOW, 30);
		on(FeatureSetting.TIRED);
		on(FeatureSetting.EXHAUST);
		HardmodeConfig.set(FeatureSetting.EXHAUST_JUMPING, 3);
		on(FeatureSetting.BLAST);
		on(FeatureSetting.COLD);
		on(FeatureSetting.BLEED);
		on(FeatureSetting.FRACTURE);
		HardmodeConfig.set(FeatureSetting.FRACTURE_FROM, 8);
		HardmodeConfig.set(FeatureSetting.FRACTURE_CHANCE, 30);
		HardmodeConfig.set(FeatureSetting.FRACTURE_SLOW, 70);
		on(FeatureSetting.TORCHES);
	}

	private static void multiplier(double strength, double speed, double followRange) {
		HardmodeConfig.setHard(HardArea.HEALTH, strength);
		HardmodeConfig.setHard(HardArea.DAMAGE, strength);
		HardmodeConfig.setHard(HardArea.SPEED, speed);
		HardmodeConfig.setHard(HardArea.FOLLOW_RANGE, followRange);
	}

	private static void zombies(double speed, double armor, double reinforcements) {
		for (String zombie : ZOMBIES) {
			set(zombie, MobStat.MOVEMENT_SPEED, speed);
			if (armor > 0) {
				set(zombie, MobStat.ARMOR, armor);
			}
			if (reinforcements > 0) {
				set(zombie, MobStat.SPAWN_REINFORCEMENTS_CHANCE, reinforcements);
			}
		}
	}

	private static void skeletons(double armor, double followRange) {
		for (String skeleton : SKELETONS) {
			if (armor > 0) {
				set(skeleton, MobStat.ARMOR, armor);
			}
			set(skeleton, MobStat.FOLLOW_RANGE, followRange);
		}
	}

	private static void spiders(double speed) {
		set("spider", MobStat.MOVEMENT_SPEED, speed);
		set("cave_spider", MobStat.MOVEMENT_SPEED, speed);
	}

	private static void creeper(double speed, double knockbackResistance) {
		set("creeper", MobStat.MOVEMENT_SPEED, speed);
		set("creeper", MobStat.KNOCKBACK_RESISTANCE, knockbackResistance);
	}

	private static void monsters(double percent) {
		on(FeatureSetting.MOB_COUNT);
		HardmodeConfig.set(FeatureSetting.SPAWN_SURFACE, percent);
		HardmodeConfig.set(FeatureSetting.SPAWN_CAVES, percent);
		HardmodeConfig.set(FeatureSetting.SPAWN_NETHER, percent);
	}

	private static void variants(double share) {
		on(FeatureSetting.VARIANTS);
		HardmodeConfig.set(FeatureSetting.VARIANT_SHARE, share);
	}

	private static void experience(double percent) {
		on(FeatureSetting.XP);
		HardmodeConfig.set(FeatureSetting.XP_GAIN, percent);
	}

	private static void on(FeatureSetting setting) {
		HardmodeConfig.set(setting, 1);
	}

	private static void set(String mob, MobStat stat, double value) {
		MobList.Entry entry = MobList.byId("minecraft:" + mob);
		if (entry != null && MobList.has(entry, stat)) {
			HardmodeConfig.setOverride(entry, stat, value);
		}
	}
}
