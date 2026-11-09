package io.github.schluid.hardmodeplus.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public enum MobStat {
	// Combat
	MAX_HEALTH(Group.COMBAT, Attributes.MAX_HEALTH),
	ATTACK_DAMAGE(Group.COMBAT, Attributes.ATTACK_DAMAGE),
	ATTACK_KNOCKBACK(Group.COMBAT, Attributes.ATTACK_KNOCKBACK),
	ARMOR(Group.COMBAT, Attributes.ARMOR),
	ARMOR_TOUGHNESS(Group.COMBAT, Attributes.ARMOR_TOUGHNESS),

	// Movement
	MOVEMENT_SPEED(Group.MOVEMENT, Attributes.MOVEMENT_SPEED),
	FLYING_SPEED(Group.MOVEMENT, Attributes.FLYING_SPEED),
	JUMP_STRENGTH(Group.MOVEMENT, Attributes.JUMP_STRENGTH),
	STEP_HEIGHT(Group.MOVEMENT, Attributes.STEP_HEIGHT),
	WATER_MOVEMENT_EFFICIENCY(Group.MOVEMENT, Attributes.WATER_MOVEMENT_EFFICIENCY),

	// Senses and body
	FOLLOW_RANGE(Group.BODY, Attributes.FOLLOW_RANGE),
	SCALE(Group.BODY, Attributes.SCALE),
	KNOCKBACK_RESISTANCE(Group.BODY, Attributes.KNOCKBACK_RESISTANCE),
	EXPLOSION_KNOCKBACK_RESISTANCE(Group.BODY, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE),
	GRAVITY(Group.BODY, Attributes.GRAVITY),

	// Other
	BURNING_TIME(Group.OTHER, Attributes.BURNING_TIME),
	FALL_DAMAGE_MULTIPLIER(Group.OTHER, Attributes.FALL_DAMAGE_MULTIPLIER),
	SAFE_FALL_DISTANCE(Group.OTHER, Attributes.SAFE_FALL_DISTANCE),
	SPAWN_REINFORCEMENTS_CHANCE(Group.OTHER, Attributes.SPAWN_REINFORCEMENTS_CHANCE);

	public enum Group {
		COMBAT, MOVEMENT, BODY, OTHER
	}

	public final Group group;
	public final Holder<Attribute> attribute;
	public final String key;

	MobStat(Group group, Holder<Attribute> attribute) {
		this.group = group;
		this.attribute = attribute;
		this.key = name().toLowerCase(Locale.ROOT);
	}

	public double min() {
		return attribute.value() instanceof RangedAttribute ranged ? ranged.getMinValue() : 0;
	}

	public double max() {
		return attribute.value() instanceof RangedAttribute ranged ? ranged.getMaxValue() : 1024;
	}

	public static MobStat byKey(String key) {
		for (MobStat stat : values()) {
			if (stat.key.equals(key)) {
				return stat;
			}
		}
		return null;
	}

	public static List<MobStat> inGroup(Group group) {
		List<MobStat> list = new ArrayList<>();
		for (MobStat stat : values()) {
			if (stat.group == group) {
				list.add(stat);
			}
		}
		return list;
	}
}
