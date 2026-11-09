package io.github.schluid.hardmodeplus.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.schluid.hardmodeplus.HardmodePlus;
import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import io.github.schluid.hardmodeplus.config.ServerSettings;
import io.github.schluid.hardmodeplus.effect.ModEffects;
import io.github.schluid.hardmodeplus.mobs.MobApplier;
import io.github.schluid.hardmodeplus.mobs.MobList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Features {
	public static final Identifier BACKSTAB_ID = Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "backstab");
	public static final Identifier EXHAUST_ID = Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "exhaustion");
	public static final Identifier BLAST_ID = Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "blast_shock");
	private static final Identifier TIRED_ID = Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "tiredness");
	private static final Identifier DEATH_ID = Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "death_penalty");
	private static final int DAY = 24000;
	private static final double TIRED_LIMIT = 90;
	private static final long HOUR = 1000;
	private static final double BEHIND = -0.5;
	private static final int SIEGE_LURE_LIMIT = 40;

	public record DeathPenalty(float points, long until) {
		public static final Codec<DeathPenalty> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.FLOAT.fieldOf("points").forGetter(DeathPenalty::points),
				Codec.LONG.fieldOf("until").forGetter(DeathPenalty::until)).apply(instance, DeathPenalty::new));
	}

	public static final AttachmentType<Long> LAST_REST = AttachmentRegistry.<Long>builder()
			.persistent(Codec.LONG)
			.buildAndRegister(Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "last_rest"));

	public static final AttachmentType<DeathPenalty> DEATH_PENALTY = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "death_penalty"),
			builder -> builder.persistent(DeathPenalty.CODEC).copyOnDeath());

	private static final Map<UUID, Long> BACKSTAB_UNTIL = new HashMap<>();
	private static final Map<UUID, Long> BLAST_UNTIL = new HashMap<>();
	private static final Map<UUID, double[]> COLD = new HashMap<>();
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
			EquipmentSlot.FEET};
	private static final int HEAT_RANGE = 3;
	private static final Map<UUID, Long> SLEEP_START = new HashMap<>();
	private static final long NIGHT_SKIPPED = 1000;
	private static final Map<UUID, Integer> STARVING = new HashMap<>();

	private static volatile boolean siegeActive;

	private Features() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			BACKSTAB_UNTIL.clear();
			BLAST_UNTIL.clear();
			COLD.clear();
			SLEEP_START.clear();
			STARVING.clear();
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (entity instanceof ServerPlayer player && !blocked && taken > 0) {
				backstab(player, source.getEntity(), source.getSourcePosition());
				blastShock(player, source, taken);
				Injuries.onDamage(player, source, base, taken);
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) {
				addDeathPenalty(player);
			}
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!alive) {
				DeathPenalty penalty = ((AttachmentTarget) oldPlayer).getAttached(DEATH_PENALTY);
				if (penalty != null) {
					((AttachmentTarget) newPlayer).setAttached(DEATH_PENALTY, penalty);
				}
				updateDeathPenalty(newPlayer, newPlayer.level().getServer().overworld().getGameTime());
			}
		});
		EntitySleepEvents.START_SLEEPING.register((entity, sleepingPos) -> {
			if (entity instanceof ServerPlayer player) {
				SLEEP_START.put(player.getUUID(), player.level().getServer().overworld().getOverworldClockTime());
			}
		});
		EntitySleepEvents.STOP_SLEEPING.register((entity, sleepingPos) -> {
			if (entity instanceof ServerPlayer player) {
				wokeUp(player);
			}
		});
	}

	public static boolean siegeActive() {
		return siegeActive;
	}

	public static void tick(MinecraftServer server) {
		long now = server.getTickCount();
		endTimed(server, BACKSTAB_UNTIL, BACKSTAB_ID, now);
		if (now % 5 == 0) {
			dazedParticles(server);
		}
		endTimed(server, BLAST_UNTIL, BLAST_ID, now);
		if (!BLAST_UNTIL.isEmpty()) {
			for (Map.Entry<UUID, Long> entry : BLAST_UNTIL.entrySet()) {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				if (player != null) {
					fadeBlast(player, entry.getValue() - now);
				}
			}
		}
		tickCold(server, now);
		Stamina.tick(server);
		if (now % 20 != 0) {
			return;
		}
		updateSiege(server);
		long gameTime = server.overworld().getGameTime();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			updateDeathPenalty(player, gameTime);
			updateStarving(player);
			updateHunger(player);
			updateLowHealth(player);
			updateTired(player);
			updateCold(player);
			ServerSettings.update(player);
			Thirst.syncFoodSaturation(player);
			Thirst.update(player);
			Injuries.update(player);
			if (siegeActive) {
				lureMonsters(player);
			}
		}
	}

	private static void backstab(ServerPlayer player, Entity attacker, Vec3 from) {
		if (!HardmodeConfig.isOn(FeatureSetting.BACKSTAB) || attacker == null || attacker == player) {
			return;
		}
		Vec3 source = from != null ? from : attacker.position();
		Vec3 look = player.getLookAngle();
		Vec3 toAttacker = new Vec3(source.x - player.getX(), 0, source.z - player.getZ());
		Vec3 forward = new Vec3(look.x, 0, look.z);
		if (toAttacker.lengthSqr() < 1.0E-4 || forward.lengthSqr() < 1.0E-4) {
			return;
		}
		if (forward.normalize().dot(toAttacker.normalize()) > BEHIND) {
			return;
		}
		double slow = HardmodeConfig.get(FeatureSetting.BACKSTAB_SLOW) / 100.0;
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.addOrUpdateTransientModifier(new AttributeModifier(BACKSTAB_ID, -slow,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			long ticks = Math.round(HardmodeConfig.get(FeatureSetting.BACKSTAB_SECONDS) * 20);
			BACKSTAB_UNTIL.put(player.getUUID(), (long) player.level().getServer().getTickCount() + ticks);
			// Icon and particles while dazed
			player.addEffect(new MobEffectInstance(ModEffects.DAZED, (int) ticks, 0, false, true, true));
		}
	}

	private static void endTimed(MinecraftServer server, Map<UUID, Long> until, Identifier id, long now) {
		if (until.isEmpty()) {
			return;
		}
		until.entrySet().removeIf(entry -> {
			if (entry.getValue() > now) {
				return false;
			}
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player != null) {
				removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), id);
			}
			return true;
		});
	}

	private static void dazedParticles(MinecraftServer server) {
		for (UUID id : BACKSTAB_UNTIL.keySet()) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player == null) {
				continue;
			}
			Vec3 look = player.getLookAngle();
			player.level().sendParticles(ParticleTypes.CRIT, player.getX() + look.x * 0.6, player.getEyeY() + 0.25,
					player.getZ() + look.z * 0.6, 3, 0.3, 0.1, 0.3, 0.0);
		}
	}

	private static boolean playing(ServerPlayer player) {
		return !player.isCreative() && !player.isSpectator() && player.isAlive();
	}

	static boolean tired(ServerPlayer player) {
		if (!HardmodeConfig.isOn(FeatureSetting.TIRED) || !playing(player)) {
			return false;
		}
		return nightsAwake(player) >= (int) Math.round(HardmodeConfig.get(FeatureSetting.TIRED_NIGHTS));
	}

	private static int nightsAwake(ServerPlayer player) {
		long today = player.level().getServer().overworld().getOverworldClockTime() / DAY;
		Long rested = ((AttachmentTarget) player).getAttached(LAST_REST);
		if (rested == null || rested > today) {
			((AttachmentTarget) player).setAttached(LAST_REST, today);
			return 0;
		}
		return (int) Math.min(Integer.MAX_VALUE, today - rested);
	}

	private static void updateTired(ServerPlayer player) {
		AttributeInstance mining = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
		// Icon while tired
		Injuries.show(player, ModEffects.TIRED, tired(player));
		if (!HardmodeConfig.isOn(FeatureSetting.TIRED) || !playing(player)) {
			removeModifier(mining, TIRED_ID);
			return;
		}
		int nights = nightsAwake(player);
		int from = (int) Math.round(HardmodeConfig.get(FeatureSetting.TIRED_NIGHTS));
		if (nights < from || mining == null) {
			removeModifier(mining, TIRED_ID);
			return;
		}
		double slow = Math.min(TIRED_LIMIT, HardmodeConfig.get(FeatureSetting.TIRED_SLOW)
				+ (nights - from) * HardmodeConfig.get(FeatureSetting.TIRED_STEP));
		AttributeModifier current = mining.getModifier(TIRED_ID);
		if (current == null || Math.abs(current.amount() + slow / 100.0) > 1.0E-4) {
			mining.addOrUpdateTransientModifier(new AttributeModifier(TIRED_ID, -slow / 100.0,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	private static void blastShock(ServerPlayer player, DamageSource source, float taken) {
		if (!HardmodeConfig.isOn(FeatureSetting.BLAST) || !source.is(DamageTypeTags.IS_EXPLOSION)
				|| taken < HardmodeConfig.get(FeatureSetting.BLAST_MIN) || !playing(player)) {
			return;
		}
		int ticks = (int) Math.round(HardmodeConfig.get(FeatureSetting.BLAST_SECONDS) * 20);
		double slow = HardmodeConfig.get(FeatureSetting.BLAST_SLOW) / 100.0;
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null && slow > 0) {
			speed.addOrUpdateTransientModifier(new AttributeModifier(BLAST_ID, -slow,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			BLAST_UNTIL.put(player.getUUID(), (long) player.level().getServer().getTickCount() + ticks);
		}
		if (HardmodeConfig.isOn(FeatureSetting.BLAST_NAUSEA)) {
			player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, ticks, 0, false, false, true));
		}
	}

	private static void fadeBlast(ServerPlayer player, long ticksLeft) {
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		double total = HardmodeConfig.get(FeatureSetting.BLAST_SECONDS) * 20;
		if (speed == null || speed.getModifier(BLAST_ID) == null || ticksLeft <= 0 || total <= 0) {
			return;
		}
		double slow = HardmodeConfig.get(FeatureSetting.BLAST_SLOW) / 100.0 * Math.min(1, ticksLeft / total);
		speed.addOrUpdateTransientModifier(new AttributeModifier(BLAST_ID, -slow,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}

	private static void updateCold(ServerPlayer player) {
		UUID id = player.getUUID();
		if (!HardmodeConfig.isOn(FeatureSetting.COLD) || !playing(player)) {
			COLD.remove(id);
			return;
		}
		double rate = coldRate(player);
		double[] state = COLD.get(id);
		if (state == null) {
			if (rate > 0) {
				COLD.put(id, new double[] {0, rate});
			}
			return;
		}
		state[1] = rate;
	}

	private static double coldRate(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos pos = player.blockPosition();
		double temperature = level.getBiome(pos).value().getBaseTemperature();
		// Like vanilla: higher up it gets colder
		temperature -= Math.max(0, pos.getY() - 80) * 0.00125;
		if (HardmodeConfig.isOn(FeatureSetting.COLD_NIGHT) && level.isDarkOutside()) {
			temperature -= 0.2;
		}
		if (temperature >= HardmodeConfig.get(FeatureSetting.COLD_TEMP)) {
			return 0;
		}
		if (HardmodeConfig.isOn(FeatureSetting.COLD_FIRE) && nearHeat(level, pos)) {
			return 0;
		}
		int leather = 0;
		for (EquipmentSlot slot : ARMOR) {
			if (player.getItemBySlot(slot).is(ItemTags.FREEZE_IMMUNE_WEARABLES)) {
				leather++;
			}
		}
		return Math.max(0, 1 - leather * HardmodeConfig.get(FeatureSetting.COLD_LEATHER) / 100.0);
	}

	private static boolean nearHeat(ServerLevel level, BlockPos pos) {
		for (BlockPos check : BlockPos.betweenClosed(pos.offset(-HEAT_RANGE, -1, -HEAT_RANGE),
				pos.offset(HEAT_RANGE, 2, HEAT_RANGE))) {
			BlockState state = level.getBlockState(check);
			if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.LAVA)
					|| state.is(Blocks.MAGMA_BLOCK) || state.is(BlockTags.CAMPFIRES) && CampfireBlock.isLitCampfire(state)) {
				return true;
			}
		}
		return false;
	}

	private static void tickCold(MinecraftServer server, long now) {
		if (COLD.isEmpty()) {
			return;
		}
		double limit = Math.max(20, HardmodeConfig.get(FeatureSetting.COLD_SECONDS) * 20);
		COLD.entrySet().removeIf(entry -> {
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				return true;
			}
			double[] state = entry.getValue();
			state[0] = state[1] > 0 ? Math.min(limit, state[0] + state[1]) : state[0] - 2;
			if (state[0] <= 0) {
				return true;
			}
			int required = player.getTicksRequiredToFreeze();
			player.setTicksFrozen((int) Math.round(required * state[0] / limit) + 2);
			if (state[0] >= limit && !player.canFreeze() && now % 40 == 0) {
				player.hurtServer(player.level(), player.damageSources().freeze(), 1);
			}
			return false;
		});
	}

	private static void wokeUp(ServerPlayer player) {
		Long start = SLEEP_START.remove(player.getUUID());
		long now = player.level().getServer().overworld().getOverworldClockTime();
		if (start == null || now - start < NIGHT_SKIPPED) {
			return;
		}
		// Slept through the night: rested again (tiredness)
		((AttachmentTarget) player).setAttached(LAST_REST, now / DAY);
		if (!HardmodeConfig.isOn(FeatureSetting.SLEEP_HEAL) || !player.isHurt()) {
			return;
		}
		player.heal((float) (HardmodeConfig.get(FeatureSetting.SLEEP_HEARTS) * 2));
		int cost = (int) Math.round(HardmodeConfig.get(FeatureSetting.SLEEP_FOOD));
		player.getFoodData().setFoodLevel(Math.max(0, player.getFoodData().getFoodLevel() - cost));
	}

	public static float healCostFactor() {
		return HardmodeConfig.isOn(FeatureSetting.HEAL) ? (float) (HardmodeConfig.get(FeatureSetting.HEAL_COST) / 100.0) : 1.0F;
	}

	public static int healFrom(int vanilla) {
		return HardmodeConfig.isOn(FeatureSetting.HEAL) ? (int) Math.round(HardmodeConfig.get(FeatureSetting.HEAL_FROM)) : vanilla;
	}

	public static int fastHealFrom(int vanilla) {
		return HardmodeConfig.isOn(FeatureSetting.HEAL) && !HardmodeConfig.isOn(FeatureSetting.FAST_HEAL) ? 21 : vanilla;
	}

	private static void addDeathPenalty(ServerPlayer player) {
		if (!HardmodeConfig.isOn(FeatureSetting.DEATH_PENALTY)) {
			return;
		}
		DeathPenalty old = ((AttachmentTarget) player).getAttached(DEATH_PENALTY);
		float max = (float) (HardmodeConfig.get(FeatureSetting.DEATH_MAX_HEARTS) * 2);
		float added = (float) (HardmodeConfig.get(FeatureSetting.DEATH_HEARTS) * 2);
		float points = Math.min(max, (old != null ? old.points() : 0) + added);
		long until = player.level().getServer().overworld().getGameTime()
				+ Math.round(HardmodeConfig.get(FeatureSetting.DEATH_HOURS) * HOUR);
		((AttachmentTarget) player).setAttached(DEATH_PENALTY, new DeathPenalty(points, until));
	}

	private static void updateDeathPenalty(ServerPlayer player, long gameTime) {
		DeathPenalty penalty = ((AttachmentTarget) player).getAttached(DEATH_PENALTY);
		if (penalty == null) {
			removeModifier(player.getAttribute(Attributes.MAX_HEALTH), DEATH_ID);
			return;
		}
		if (!HardmodeConfig.isOn(FeatureSetting.DEATH_PENALTY) || gameTime >= penalty.until()) {
			((AttachmentTarget) player).removeAttached(DEATH_PENALTY);
			removeModifier(player.getAttribute(Attributes.MAX_HEALTH), DEATH_ID);
			return;
		}
		applyDeathModifier(player, penalty.points());
	}

	private static void applyDeathModifier(ServerPlayer player, float points) {
		AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
		if (health == null) {
			return;
		}
		AttributeModifier current = health.getModifier(DEATH_ID);
		if (current == null || Math.abs(current.amount() + points) > 1.0E-4) {
			health.addOrUpdateTransientModifier(new AttributeModifier(DEATH_ID, -points, AttributeModifier.Operation.ADD_VALUE));
		}
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
	}

	public static float exhaustionFactor(boolean sprinting) {
		if (!HardmodeConfig.isOn(FeatureSetting.FOOD)) {
			return 1.0F;
		}
		FeatureSetting setting = sprinting ? FeatureSetting.FOOD_SPRINT : FeatureSetting.FOOD_USE;
		return (float) (HardmodeConfig.get(setting) / 100.0);
	}

	private static void updateHunger(ServerPlayer player) {
		if (!HardmodeConfig.isOn(FeatureSetting.FOOD) || player.isCreative() || player.isSpectator() || !player.isAlive()) {
			return;
		}
		double perMinute = HardmodeConfig.get(FeatureSetting.FOOD_IDLE);
		if (perMinute > 0) {
			player.causeFoodExhaustion((float) (perMinute * 4.0 / 60.0));
		}
	}

	public static boolean replacesStarvation() {
		return HardmodeConfig.isOn(FeatureSetting.FOOD);
	}

	private static void updateStarving(ServerPlayer player) {
		UUID id = player.getUUID();
		boolean empty = player.getFoodData().getFoodLevel() <= 0 && !player.isCreative() && !player.isSpectator()
				&& player.isAlive();
		if (!HardmodeConfig.isOn(FeatureSetting.FOOD) || !empty) {
			STARVING.remove(id);
			return;
		}
		int ticks = STARVING.merge(id, 20, Integer::sum);
		int delay = (int) Math.round(HardmodeConfig.get(FeatureSetting.STARVE_DELAY) * 1200);
		int interval = (int) Math.max(20, Math.round(HardmodeConfig.get(FeatureSetting.STARVE_INTERVAL) * 20));
		double damage = HardmodeConfig.get(FeatureSetting.STARVE_DAMAGE);
		if (ticks >= delay && damage > 0 && (ticks - delay) % interval < 20) {
			ServerLevel level = player.level();
			player.hurtServer(level, player.damageSources().starve(), (float) damage);
		}
	}

	private static void updateLowHealth(ServerPlayer player) {
		if (!HardmodeConfig.isOn(FeatureSetting.LOW_HEALTH_BLIND) || player.isCreative() || player.isSpectator()
				|| !player.isAlive()) {
			return;
		}
		if (player.getHealth() <= HardmodeConfig.get(FeatureSetting.LOW_HEALTH_BLIND_AT) * 2) {
			player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, true, false, false));
		}
	}

	private static void lureMonsters(ServerPlayer player) {
		if (!HardmodeConfig.isOn(FeatureSetting.SIEGE_ATTRACT) || player.isCreative() || player.isSpectator()
				|| !player.isAlive() || player.level().dimension() != Level.OVERWORLD) {
			return;
		}
		double range = HardmodeConfig.get(FeatureSetting.SIEGE_RANGE);
		AABB area = player.getBoundingBox().inflate(range, range / 2, range);
		int lured = 0;
		for (Mob mob : player.level().getEntitiesOfClass(Mob.class, area)) {
			if (lured >= SIEGE_LURE_LIMIT) {
				break;
			}
			MobList.Entry entry = MobList.find(mob.getType());
			if (entry == null || entry.kind() != MobList.Kind.HOSTILE || mob.isNoAi()) {
				continue;
			}
			if (mob.getTarget() == null) {
				mob.setTarget(player);
			}
			if (mob.getTarget() == player && mob.getNavigation().isDone()) {
				mob.getNavigation().moveTo(player, 1.0);
			}
			lured++;
		}
	}

	private static void updateSiege(MinecraftServer server) {
		boolean active = false;
		if (HardmodeConfig.isOn(FeatureSetting.SIEGE)) {
			ServerLevel overworld = server.overworld();
			long day = overworld.getOverworldClockTime() / 24000L;
			long every = Math.max(2, Math.round(HardmodeConfig.get(FeatureSetting.SIEGE_DAYS)));
			active = (day + 1) % every == 0 && overworld.isDarkOutside();
		}
		if (active == siegeActive) {
			return;
		}
		siegeActive = active;
		MobApplier.applyAll(server);
		if (HardmodeConfig.isOn(FeatureSetting.SIEGE_MESSAGE)) {
			server.getPlayerList().broadcastSystemMessage(Component.translatable(active
					? "hardmodeplus.siege.start" : "hardmodeplus.siege.end")
					.withStyle(active ? ChatFormatting.DARK_RED : ChatFormatting.GREEN), false);
		}
	}

	public static boolean blocksSurfaceSpawn(ServerLevel level, int blockY, int surfaceY) {
		if (!HardmodeConfig.isOn(FeatureSetting.SAFE_SURFACE) || level.dimension() != Level.OVERWORLD
				|| blockY < surfaceY) {
			return false;
		}
		ServerLevel end = level.getServer().getLevel(Level.END);
		EnderDragonFight fight = end != null ? end.getDragonFight() : null;
		return fight != null && fight.hasPreviouslyKilledDragon();
	}

	private static void removeModifier(AttributeInstance instance, Identifier id) {
		if (instance != null && instance.getModifier(id) != null) {
			instance.removeModifier(id);
		}
	}
}
