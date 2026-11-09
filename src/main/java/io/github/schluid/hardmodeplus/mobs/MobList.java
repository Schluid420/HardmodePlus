package io.github.schluid.hardmodeplus.mobs;

import io.github.schluid.hardmodeplus.config.MobStat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

public final class MobList {
	public enum Kind {
		HOSTILE, NEUTRAL, BOSS, PASSIVE;

		public boolean hasMultiplier() {
			return this != PASSIVE;
		}
	}

	public record Entry(String id, EntityType<?> type, Kind kind) {
	}

	private static final Set<String> BOSSES = Set.of("minecraft:ender_dragon", "minecraft:wither");

	private static final Set<String> NEUTRAL = Set.of(
			"minecraft:bee", "minecraft:dolphin", "minecraft:goat", "minecraft:iron_golem",
			"minecraft:llama", "minecraft:trader_llama", "minecraft:nautilus", "minecraft:panda",
			"minecraft:polar_bear", "minecraft:pufferfish", "minecraft:wolf");

	private static final Set<String> PASSIVE_MISC = Set.of(
			"minecraft:villager", "minecraft:snow_golem", "minecraft:copper_golem");

	private static final Set<MobCategory> PASSIVE_CATEGORIES = Set.of(
			MobCategory.CREATURE, MobCategory.AMBIENT, MobCategory.AXOLOTLS, MobCategory.WATER_CREATURE,
			MobCategory.WATER_AMBIENT, MobCategory.UNDERGROUND_WATER_CREATURE);

	private static List<Entry> entries;
	private static Map<EntityType<?>, Entry> byType;

	private MobList() {
	}

	public static synchronized List<Entry> all() {
		if (entries == null) {
			List<Entry> list = new ArrayList<>();
			for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
				Kind kind = kindOf(type);
				if (kind != null && DefaultAttributes.hasSupplier(type)) {
					list.add(new Entry(EntityType.getKey(type).toString(), type, kind));
				}
			}
			list.sort(Comparator.comparing(Entry::kind).thenComparing(Entry::id));
			entries = List.copyOf(list);
			Map<EntityType<?>, Entry> map = new IdentityHashMap<>();
			for (Entry entry : entries) {
				map.put(entry.type(), entry);
			}
			byType = map;
		}
		return entries;
	}

	private static Kind kindOf(EntityType<?> type) {
		String id = EntityType.getKey(type).toString();
		if (BOSSES.contains(id)) {
			return Kind.BOSS;
		}
		if (NEUTRAL.contains(id)) {
			return Kind.NEUTRAL;
		}
		if (type.getCategory() == MobCategory.MONSTER) {
			return Kind.HOSTILE;
		}
		if (PASSIVE_CATEGORIES.contains(type.getCategory()) || PASSIVE_MISC.contains(id)) {
			return Kind.PASSIVE;
		}
		return null;
	}

	public static Entry find(EntityType<?> type) {
		all();
		return byType.get(type);
	}

	public static Entry byId(String id) {
		for (Entry entry : all()) {
			if (entry.id().equals(id)) {
				return entry;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static AttributeSupplier supplier(EntityType<?> type) {
		return DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
	}

	private static final Set<String> NO_MELEE = Set.of("minecraft:creeper", "minecraft:witch", "minecraft:evoker");

	public static boolean has(Entry entry, MobStat stat) {
		if (stat == MobStat.ATTACK_DAMAGE && NO_MELEE.contains(entry.id())) {
			return false;
		}
		return supplier(entry.type()).hasAttribute(stat.attribute);
	}

	public static double vanilla(Entry entry, MobStat stat) {
		AttributeSupplier supplier = supplier(entry.type());
		return supplier.hasAttribute(stat.attribute) ? supplier.getBaseValue(stat.attribute) : 0;
	}

	public static String iconItem(Entry entry) {
		Identifier id = Identifier.parse(entry.id());
		Identifier egg = Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_spawn_egg");
		return BuiltInRegistries.ITEM.containsKey(egg) ? egg.toString() : null;
	}
}
