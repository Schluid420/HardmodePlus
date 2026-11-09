package io.github.schluid.hardmodeplus.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.schluid.hardmodeplus.HardmodePlus;
import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import io.github.schluid.hardmodeplus.effect.ModEffects;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

// Bleeding and broken bones, plus the treatment items
public final class Injuries {
	public static final int NONE = 0;
	public static final int LIGHT = 1;
	public static final int HEAVY = 2;
	private static final double LIMP_STEP = 0.5;

	public record Injury(int bleeding, boolean fracture, long castUntil, long splintUntil) {
		public static final Injury HEALTHY = new Injury(NONE, false, 0, 0);
		public static final Codec<Injury> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.fieldOf("bleeding").forGetter(Injury::bleeding),
				Codec.BOOL.fieldOf("fracture").forGetter(Injury::fracture),
				Codec.LONG.optionalFieldOf("cast_until", 0L).forGetter(Injury::castUntil),
				Codec.LONG.optionalFieldOf("splint_until", 0L).forGetter(Injury::splintUntil)).apply(instance, Injury::new));

		Injury(int bleeding, boolean fracture) {
			this(bleeding, fracture, 0, 0);
		}

		Injury withBleeding(int value) {
			return new Injury(value, fracture, castUntil, splintUntil);
		}

		boolean splinted() {
			return fracture && splintUntil > 0 && castUntil <= 0;
		}

		boolean inCast() {
			return fracture && castUntil > 0;
		}
	}

	public static final AttachmentType<Injury> INJURY = AttachmentRegistry.<Injury>builder()
			.persistent(Injury.CODEC)
			.buildAndRegister(Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "injury"));

	public static final Identifier FRACTURE_ID = Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "fracture");

	private static final Map<UUID, Integer> BLEED_TIMER = new HashMap<>();

	private Injuries() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			BLEED_TIMER.clear();
		});
	}

	public static Injury get(ServerPlayer player) {
		Injury injury = ((AttachmentTarget) player).getAttached(INJURY);
		return injury != null ? injury : Injury.HEALTHY;
	}

	private static void set(ServerPlayer player, Injury injury) {
		if (injury.bleeding() == NONE && !injury.fracture()) {
			((AttachmentTarget) player).removeAttached(INJURY);
		} else {
			((AttachmentTarget) player).setAttached(INJURY, injury);
		}
	}

	private static boolean playing(ServerPlayer player) {
		return !player.isCreative() && !player.isSpectator() && player.isAlive();
	}

	static void onDamage(ServerPlayer player, DamageSource source, float base, float taken) {
		if (!playing(player)) {
			return;
		}
		Injury injury = get(player);
		if (HardmodeConfig.isOn(FeatureSetting.BLEED) && source.getEntity() instanceof LivingEntity attacker
				&& attacker != player && !source.is(DamageTypeTags.IS_EXPLOSION)
				&& taken >= HardmodeConfig.get(FeatureSetting.BLEED_MIN) && injury.bleeding() < HEAVY) {
			double roll = player.getRandom().nextDouble() * 100;
			if (roll < HardmodeConfig.get(FeatureSetting.HEAVY_CHANCE)) {
				injury = injury.withBleeding(HEAVY);
			} else if (injury.bleeding() == NONE
					&& roll < HardmodeConfig.get(FeatureSetting.HEAVY_CHANCE) + HardmodeConfig.get(FeatureSetting.BLEED_CHANCE)) {
				injury = injury.withBleeding(LIGHT);
			}
		}
		if (HardmodeConfig.isOn(FeatureSetting.FRACTURE) && source.is(DamageTypeTags.IS_FALL) && !injury.fracture()) {
			// Fall damage is the fall height minus 3 safe blocks
			double blocks = base + 3;
			boolean broken = blocks >= HardmodeConfig.get(FeatureSetting.FRACTURE_SURE)
					|| blocks >= HardmodeConfig.get(FeatureSetting.FRACTURE_FROM)
							&& player.getRandom().nextDouble() * 100 < HardmodeConfig.get(FeatureSetting.FRACTURE_CHANCE);
			if (broken) {
				injury = new Injury(injury.bleeding(), true, 0, 0);
			}
		}
		if (!injury.equals(get(player))) {
			set(player, injury);
			update(player);
		}
	}

	public static boolean helps(ServerPlayer player, Treatment treatment) {
		Injury injury = get(player);
		return switch (treatment) {
			case BANDAGE -> injury.bleeding() == LIGHT;
			case HERBAL -> injury.bleeding() == LIGHT;
			case TOURNIQUET -> injury.bleeding() != NONE;
			case SPLINT -> injury.fracture() && !injury.inCast() && !injury.splinted();
			case CAST -> injury.fracture() && !injury.inCast();
		};
	}

	public static void treat(ServerPlayer player, Treatment treatment) {
		if (!helps(player, treatment)) {
			return;
		}
		Injury injury = get(player);
		switch (treatment) {
			case BANDAGE -> {
				injury = injury.withBleeding(NONE);
				player.heal((float) (HardmodeConfig.get(FeatureSetting.BANDAGE_HEARTS) * 2));
			}
			case TOURNIQUET -> {
				injury = injury.withBleeding(NONE);
				player.heal((float) (HardmodeConfig.get(FeatureSetting.TOURNIQUET_HEARTS) * 2));
			}
			case HERBAL -> {
				if (injury.bleeding() == LIGHT) {
					injury = injury.withBleeding(NONE);
				}
				player.heal((float) (HardmodeConfig.get(FeatureSetting.HERBAL_HEARTS) * 2));
			}
			case SPLINT -> {
				long time = Math.round(HardmodeConfig.get(FeatureSetting.SPLINT_DAYS) * DAY);
				injury = new Injury(injury.bleeding(), true, 0, gameTime(player) + Math.max(1, time));
			}
			case CAST -> {
				long hours = Math.round(HardmodeConfig.get(FeatureSetting.CAST_HOURS) * HOUR);
				injury = HardmodeConfig.isOn(FeatureSetting.CAST_INSTANT)
						? new Injury(injury.bleeding(), false, 0, 0)
						: new Injury(injury.bleeding(), true, gameTime(player) + Math.max(1, hours), 0);
			}
		}
		set(player, injury);
		update(player);
	}

	public enum Treatment {
		BANDAGE, HERBAL, TOURNIQUET, SPLINT, CAST
	}

	private static final long HOUR = 1000;
	private static final long DAY = 24000;

	private static long gameTime(ServerPlayer player) {
		return player.level().getServer().overworld().getGameTime();
	}

	static void update(ServerPlayer player) {
		Injury injury = get(player);
		boolean bleedOn = HardmodeConfig.isOn(FeatureSetting.BLEED);
		boolean fractureOn = HardmodeConfig.isOn(FeatureSetting.FRACTURE);
		if (!player.isAlive() || player.isSpectator()) {
			return;
		}
		if (injury.bleeding() != NONE && !bleedOn || injury.fracture() && !fractureOn) {
			injury = fractureOn ? injury.withBleeding(NONE) : new Injury(bleedOn ? injury.bleeding() : NONE, false, 0, 0);
			set(player, injury);
		}
		// The cast has done its work: the bone is healed
		if (injury.inCast() && gameTime(player) >= injury.castUntil()) {
			injury = new Injury(injury.bleeding(), false, 0, 0);
			set(player, injury);
		}
		// The splint gave way: the bone is loose again
		if (injury.splinted() && gameTime(player) >= injury.splintUntil()) {
			injury = new Injury(injury.bleeding(), true, 0, 0);
			set(player, injury);
			player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8F, 0.9F);
			player.sendOverlayMessage(Component.translatable("hardmodeplus.message.splint_broke"));
		}

		show(player, ModEffects.BLEEDING, injury.bleeding() == LIGHT);
		show(player, ModEffects.HEAVY_BLEEDING, injury.bleeding() == HEAVY);
		show(player, ModEffects.FRACTURE, injury.fracture() && !injury.inCast() && !injury.splinted());
		showTimed(player, ModEffects.SPLINTED, injury.splinted() ? injury.splintUntil() - gameTime(player) : 0);
		showTimed(player, ModEffects.IN_CAST, injury.inCast() ? injury.castUntil() - gameTime(player) : 0);

		// Bleeding damage
		UUID id = player.getUUID();
		if (injury.bleeding() == NONE || player.isCreative()) {
			BLEED_TIMER.remove(id);
		} else {
			boolean heavy = injury.bleeding() == HEAVY;
			int interval = (int) Math.max(1, Math.round(HardmodeConfig.get(heavy ? FeatureSetting.HEAVY_INTERVAL
					: FeatureSetting.BLEED_INTERVAL)));
			int seconds = BLEED_TIMER.merge(id, 1, Integer::sum);
			if (seconds >= interval) {
				BLEED_TIMER.put(id, 0);
				double damage = HardmodeConfig.get(heavy ? FeatureSetting.HEAVY_DAMAGE : FeatureSetting.BLEED_DAMAGE);
				if (damage > 0) {
					player.hurtServer(player.level(), player.damageSources().generic(), (float) damage);
				}
			}
		}

		// A splint or a cast halves the slowdown.
		double slow = HardmodeConfig.get(FeatureSetting.FRACTURE_SLOW);
		if (injury.inCast() || injury.splinted()) {
			slow /= 2;
		}
		modifier(player.getAttribute(Attributes.MOVEMENT_SPEED), injury.fracture(), slow);
		modifier(player.getAttribute(Attributes.JUMP_STRENGTH), injury.fracture(), 100);
		step(player.getAttribute(Attributes.STEP_HEIGHT), injury.fracture());
	}

	private static void step(AttributeInstance instance, boolean active) {
		if (instance == null) {
			return;
		}
		if (!active) {
			if (instance.getModifier(FRACTURE_ID) != null) {
				instance.removeModifier(FRACTURE_ID);
			}
		} else if (instance.getModifier(FRACTURE_ID) == null) {
			instance.addOrUpdateTransientModifier(new AttributeModifier(FRACTURE_ID, LIMP_STEP,
					AttributeModifier.Operation.ADD_VALUE));
		}
	}

	static void show(ServerPlayer player, Holder<MobEffect> effect, boolean active) {
		if (active && !player.hasEffect(effect)) {
			player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, 0, false, true, true));
		} else if (!active && player.hasEffect(effect)) {
			player.removeEffect(effect);
		}
	}

	private static void showTimed(ServerPlayer player, Holder<MobEffect> effect, long ticks) {
		MobEffectInstance current = player.getEffect(effect);
		if (ticks <= 0) {
			if (current != null) {
				player.removeEffect(effect);
			}
			return;
		}
		if (current == null || Math.abs(current.getDuration() - ticks) > 40) {
			player.removeEffect(effect);
			player.addEffect(new MobEffectInstance(effect, (int) Math.min(Integer.MAX_VALUE, ticks), 0, false, true, true));
		}
	}

	private static void modifier(AttributeInstance instance, boolean active, double percent) {
		if (instance == null) {
			return;
		}
		AttributeModifier current = instance.getModifier(FRACTURE_ID);
		if (!active || percent <= 0) {
			if (current != null) {
				instance.removeModifier(FRACTURE_ID);
			}
			return;
		}
		double amount = -percent / 100.0;
		if (current == null || Math.abs(current.amount() - amount) > 1.0E-4) {
			instance.addOrUpdateTransientModifier(new AttributeModifier(FRACTURE_ID, amount,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}
}
