package io.github.schluid.hardmodeplus.mobs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.schluid.hardmodeplus.HardmodePlus;
import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

// Fast, tough, strong and random variants, rolled once per mob on spawn
public final class Variants {
	public static final int NORMAL = 0;
	public static final int FAST = 1;
	public static final int TOUGH = 2;
	public static final int STRONG = 3;
	public static final int RANDOM = 4;
	public static final String[] NAMES = {"normal", "fast", "tough", "strong", "random"};

	public record Variant(int kind, float health, float speed, float damage) {
		public static final Codec<Variant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.fieldOf("kind").forGetter(Variant::kind),
				Codec.FLOAT.fieldOf("health").forGetter(Variant::health),
				Codec.FLOAT.fieldOf("speed").forGetter(Variant::speed),
				Codec.FLOAT.fieldOf("damage").forGetter(Variant::damage)).apply(instance, Variant::new));
		public static final StreamCodec<RegistryFriendlyByteBuf, Variant> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Variant::kind, ByteBufCodecs.FLOAT, Variant::health,
				ByteBufCodecs.FLOAT, Variant::speed, ByteBufCodecs.FLOAT, Variant::damage, Variant::new);
	}

	public static final AttachmentType<Variant> VARIANT = AttachmentRegistry.<Variant>builder()
			.persistent(Variant.CODEC)
			.syncWith(Variant.STREAM_CODEC, AttachmentSyncPredicate.all())
			.buildAndRegister(Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "variant"));

	public static final AttachmentType<Boolean> HIDDEN = AttachmentRegistry.<Boolean>builder()
			.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all())
			.buildAndRegister(Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "variant_hidden"));

	public static final Identifier MODIFIER = Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "variant");
	private static final double MIN_SIZE = -50;
	private static final double MAX_SIZE = 30;
	private static final double EPSILON = 1.0E-6;

	private Variants() {
	}

	public static void register() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> apply(entity));
	}

	public static void applyAll(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				apply(entity);
			}
		}
	}

	private static final Set<EntityType<?>> WITH_SKINS = Set.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON,
			EntityTypes.SPIDER, EntityTypes.CAVE_SPIDER, EntityTypes.CREEPER);

	private static boolean eligible(Mob mob) {
		return WITH_SKINS.contains(mob.getType());
	}

	private static boolean monster(Mob mob) {
		MobList.Entry entry = MobList.find(mob.getType());
		return entry != null && entry.kind() == MobList.Kind.HOSTILE;
	}

	public static Variant get(Entity entity) {
		return ((AttachmentTarget) entity).getAttached(VARIANT);
	}

	public static int shownKind(Entity entity) {
		Variant variant = get(entity);
		if (variant == null || Boolean.TRUE.equals(((AttachmentTarget) entity).getAttached(HIDDEN))) {
			return NORMAL;
		}
		return variant.kind();
	}

	private static boolean allowedHere(Mob mob) {
		if (!HardmodeConfig.isOn(FeatureSetting.VARIANT_UNDERGROUND) || mob.level().dimension() != Level.OVERWORLD) {
			return true;
		}
		if (mob.getBlockY() >= HardmodeConfig.get(FeatureSetting.VARIANT_UNDERGROUND_Y)) {
			return false;
		}
		return mob.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, mob.getBlockX(), mob.getBlockZ())
				> mob.getBlockY();
	}

	private static final FeatureSetting[] WEIGHTS = {FeatureSetting.FAST_WEIGHT, FeatureSetting.TOUGH_WEIGHT,
			FeatureSetting.STRONG_WEIGHT, FeatureSetting.RANDOM_WEIGHT};

	public static double sharePercent(FeatureSetting weight) {
		double sum = 0;
		for (FeatureSetting w : WEIGHTS) {
			sum += HardmodeConfig.get(w);
		}
		return sum <= 0 ? 0 : HardmodeConfig.get(FeatureSetting.VARIANT_SHARE) * HardmodeConfig.get(weight) / sum;
	}

	private static Variant roll(RandomSource random) {
		int kind = NORMAL;
		double sum = 0;
		for (FeatureSetting w : WEIGHTS) {
			sum += HardmodeConfig.get(w);
		}
		if (sum > 0 && random.nextDouble() * 100 < HardmodeConfig.get(FeatureSetting.VARIANT_SHARE)) {
			double pick = random.nextDouble() * sum;
			for (int i = 0; i < WEIGHTS.length; i++) {
				pick -= HardmodeConfig.get(WEIGHTS[i]);
				if (pick < 0) {
					kind = FAST + i;
					break;
				}
			}
		}
		return new Variant(kind, random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1);
	}

	public static void apply(Entity entity) {
		if (!(entity instanceof Mob mob)) {
			return;
		}
		boolean skinned = eligible(mob);
		boolean on = HardmodeConfig.isOn(FeatureSetting.VARIANTS) && (skinned || monster(mob));
		Variant variant = get(mob);
		boolean fresh = false;
		if (on && variant == null) {
			variant = roll(mob.getRandom());
			if (!skinned || !allowedHere(mob)) {
				variant = new Variant(NORMAL, variant.health(), variant.speed(), variant.damage());
			}
			((AttachmentTarget) mob).setAttached(VARIANT, variant);
			fresh = true;
		} else if (on && !skinned && variant.kind() != NORMAL) {
			variant = new Variant(NORMAL, variant.health(), variant.speed(), variant.damage());
			((AttachmentTarget) mob).setAttached(VARIANT, variant);
		}
		if (!on || variant == null) {
			clear(mob);
			if (variant != null) {
				((AttachmentTarget) mob).setAttached(HIDDEN, true);
			}
			return;
		}
		if (((AttachmentTarget) mob).getAttached(HIDDEN) != null) {
			((AttachmentTarget) mob).removeAttached(HIDDEN);
		}

		double health = 0;
		double speed = 0;
		double damage = 0;
		double size = 0;
		double knockback = 0;
		switch (variant.kind()) {
			case FAST -> {
				speed = HardmodeConfig.get(FeatureSetting.FAST_SPEED);
				health = HardmodeConfig.get(FeatureSetting.FAST_HEALTH);
				size = HardmodeConfig.get(FeatureSetting.FAST_SIZE);
			}
			case TOUGH -> {
				health = HardmodeConfig.get(FeatureSetting.TOUGH_HEALTH);
				speed = HardmodeConfig.get(FeatureSetting.TOUGH_SPEED);
				knockback = HardmodeConfig.get(FeatureSetting.TOUGH_KNOCKBACK);
				size = HardmodeConfig.get(FeatureSetting.TOUGH_SIZE);
			}
			case STRONG -> {
				damage = HardmodeConfig.get(FeatureSetting.STRONG_DAMAGE);
				size = HardmodeConfig.get(FeatureSetting.STRONG_SIZE);
			}
			case RANDOM -> {
				// Own random values, from much weaker to much stronger
				double range = HardmodeConfig.get(FeatureSetting.RANDOM_RANGE);
				health = variant.health() * range;
				speed = variant.speed() * range;
				size = variant.damage() * 12;
			}
			default -> {
			}
		}
		if (HardmodeConfig.isOn(FeatureSetting.VARIANT_RANDOM) && variant.kind() != RANDOM) {
			double spread = HardmodeConfig.get(FeatureSetting.VARIANT_SPREAD);
			health += variant.health() * spread;
			speed += variant.speed() * spread;
			// Small size spread: a quarter of the setting, at most +-5 %
			size += variant.damage() * Math.min(5, spread / 4);
		}
		if (!HardmodeConfig.isOn(FeatureSetting.VARIANT_SCALE)) {
			size = 0;
		}
		size = Math.max(MIN_SIZE, Math.min(MAX_SIZE, size));

		double growth = Growth.factor();
		health *= growth;
		speed *= growth;
		knockback *= growth;
		float oldMax = mob.getMaxHealth();
		float oldHealth = mob.getHealth();
		set(mob, Attributes.MAX_HEALTH, Math.max(-90, health) / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		set(mob, Attributes.MOVEMENT_SPEED, Math.max(-90, speed) / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		set(mob, Attributes.FLYING_SPEED, Math.max(-90, speed) / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		set(mob, Attributes.SCALE, size / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		set(mob, Attributes.KNOCKBACK_RESISTANCE, knockback / 100.0, AttributeModifier.Operation.ADD_VALUE);
		float newMax = mob.getMaxHealth();
		if (fresh) {
			mob.setHealth(newMax);
		} else if (Math.abs(newMax - oldMax) > EPSILON && oldMax > 0 && oldHealth > 0) {
			mob.setHealth(Math.min(newMax, oldHealth * newMax / oldMax));
		}

	}

	private static void clear(Mob mob) {
		for (Holder<Attribute> attribute : List.of(Attributes.MAX_HEALTH, Attributes.MOVEMENT_SPEED,
				Attributes.FLYING_SPEED, Attributes.SCALE, Attributes.KNOCKBACK_RESISTANCE)) {
			AttributeInstance instance = mob.getAttribute(attribute);
			if (instance != null && instance.getModifier(MODIFIER) != null) {
				instance.removeModifier(MODIFIER);
			}
		}
		if (mob.getHealth() > mob.getMaxHealth()) {
			mob.setHealth(mob.getMaxHealth());
		}
	}

	private static void set(LivingEntity living, Holder<Attribute> attribute, double amount,
			AttributeModifier.Operation operation) {
		AttributeInstance instance = living.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		AttributeModifier current = instance.getModifier(MODIFIER);
		if (Math.abs(amount) < EPSILON) {
			if (current != null) {
				instance.removeModifier(MODIFIER);
			}
			return;
		}
		if (current == null || Math.abs(current.amount() - amount) > EPSILON) {
			instance.addOrReplacePermanentModifier(new AttributeModifier(MODIFIER, amount, operation));
		}
	}

	public static float damageFactor(Entity attacker) {
		if (!HardmodeConfig.isOn(FeatureSetting.VARIANTS) || !(attacker instanceof Mob)) {
			return 1.0F;
		}
		Variant variant = get(attacker);
		if (variant == null || Boolean.TRUE.equals(((AttachmentTarget) attacker).getAttached(HIDDEN))) {
			return 1.0F;
		}
		double percent = variant.kind() == STRONG ? HardmodeConfig.get(FeatureSetting.STRONG_DAMAGE) : 0;
		if (variant.kind() == RANDOM) {
			percent += variant.damage() * HardmodeConfig.get(FeatureSetting.RANDOM_RANGE);
		} else if (HardmodeConfig.isOn(FeatureSetting.VARIANT_RANDOM)) {
			percent += variant.damage() * HardmodeConfig.get(FeatureSetting.VARIANT_SPREAD);
		}
		return (float) Math.max(0.1, 1 + percent * Growth.factor() / 100.0);
	}

	private static final ItemLike[] RARE = {Items.IRON_NUGGET, Items.GOLD_NUGGET, Items.IRON_INGOT, Items.GOLD_INGOT,
			Items.EMERALD};
	private static final int[] RARE_WEIGHT = {50, 35, 6, 4, 5};

	private static boolean lootVariant(Entity entity) {
		if (!HardmodeConfig.isOn(FeatureSetting.VARIANTS) || !HardmodeConfig.isOn(FeatureSetting.VARIANT_LOOT)
				|| !(entity instanceof Mob)) {
			return false;
		}
		return shownKind(entity) != NORMAL;
	}

	public static int experience(Entity entity, int xp) {
		if (xp <= 0 || !lootVariant(entity)) {
			return xp;
		}
		return (int) Math.round(xp * (1 + HardmodeConfig.get(FeatureSetting.LOOT_XP) / 100.0));
	}

	public static void extraLoot(LivingEntity mob, ServerLevel level, boolean byPlayer, Runnable dropAgain) {
		if (!byPlayer || !lootVariant(mob)) {
			return;
		}
		RandomSource random = mob.getRandom();
		if (random.nextDouble() * 100 < HardmodeConfig.get(FeatureSetting.LOOT_DOUBLE)) {
			dropAgain.run();
		}
		if (random.nextDouble() * 100 < HardmodeConfig.get(FeatureSetting.LOOT_RARE)) {
			int roll = random.nextInt(100);
			for (int i = 0; i < RARE.length; i++) {
				roll -= RARE_WEIGHT[i];
				if (roll < 0) {
					// Nuggets come in 1 to 3, everything else alone
					int count = i < 2 ? 1 + random.nextInt(3) : 1;
					mob.spawnAtLocation(level, new ItemStack(RARE[i], count));
					break;
				}
			}
		}
	}
}
