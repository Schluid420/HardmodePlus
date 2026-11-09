package io.github.schluid.hardmodeplus.feature;

import com.mojang.serialization.Codec;
import io.github.schluid.hardmodeplus.HardmodePlus;
import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// Thirst bar, works like the food bar but with its own values
public final class Thirst {
	public static final int MAX = 20;

	public static final AttachmentType<Integer> THIRST = AttachmentRegistry.<Integer>builder()
			.persistent(Codec.INT)
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly())
			.buildAndRegister(Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "thirst"));

	public static final AttachmentType<Double> SATURATION = AttachmentRegistry.<Double>builder()
			.persistent(Codec.DOUBLE)
			.syncWith(ByteBufCodecs.DOUBLE, AttachmentSyncPredicate.targetOnly())
			.buildAndRegister(Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "thirst_saturation"));

	public static final AttachmentType<Double> FOOD_SATURATION = AttachmentRegistry.<Double>builder()
			.syncWith(ByteBufCodecs.DOUBLE, AttachmentSyncPredicate.targetOnly())
			.buildAndRegister(Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "food_saturation"));

	private static final float HOT = 1.0F;

	private static final Map<UUID, Double> USED = new HashMap<>();
	private static final Map<UUID, Integer> DRY = new HashMap<>();

	private Thirst() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			USED.clear();
			DRY.clear();
		});
	}

	public static int get(Player player) {
		Integer value = ((AttachmentTarget) player).getAttached(THIRST);
		return value != null ? value : MAX;
	}

	private static void set(ServerPlayer player, int value) {
		int clamped = Math.max(0, Math.min(MAX, value));
		if (clamped != get(player)) {
			((AttachmentTarget) player).setAttached(THIRST, clamped);
		}
	}

	private static boolean active(ServerPlayer player) {
		return HardmodeConfig.isOn(FeatureSetting.THIRST) && !player.isCreative() && !player.isSpectator()
				&& player.isAlive();
	}

	private static double saturation(ServerPlayer player) {
		Double value = ((AttachmentTarget) player).getAttached(SATURATION);
		return value != null && Double.isFinite(value) ? value : 0;
	}

	private static void setSaturation(ServerPlayer player, double value) {
		if (value <= 0) {
			((AttachmentTarget) player).removeAttached(SATURATION);
		} else {
			((AttachmentTarget) player).setAttached(SATURATION, value);
		}
	}

	public static void drink(ServerPlayer player, double amount, double saturation) {
		if (!HardmodeConfig.isOn(FeatureSetting.THIRST) || amount <= 0 && saturation <= 0) {
			return;
		}
		set(player, get(player) + (int) Math.round(amount));
		setSaturation(player, Math.min(get(player), saturation(player) + saturation));
		DRY.remove(player.getUUID());
	}

	static void exhausted(ServerPlayer player) {
		if (active(player)) {
			use(player, HardmodeConfig.get(FeatureSetting.THIRST_EXHAUST) * firstDayFactor(player));
		}
	}

	public static double shownSaturation(Player player) {
		Double value = ((AttachmentTarget) player).getAttached(SATURATION);
		return value != null ? value : 0;
	}

	private static double firstDayFactor(ServerPlayer player) {
		if (player.level().getServer().overworld().getGameTime() >= 24000L) {
			return 1;
		}
		// At least 50 %, also if an older config holds a smaller value
		return Math.max(50, HardmodeConfig.get(FeatureSetting.THIRST_FIRST_DAY)) / 100.0;
	}

	public static double staminaFactor(ServerPlayer player) {
		if (!active(player) || !HardmodeConfig.isOn(FeatureSetting.THIRST_STAMINA)) {
			return 1;
		}
		double from = HardmodeConfig.get(FeatureSetting.THIRST_STAMINA_FROM);
		int thirst = get(player);
		if (thirst >= from) {
			return 1;
		}
		double depth = 1 - thirst / from;
		return 1 - HardmodeConfig.get(FeatureSetting.THIRST_STAMINA_REGEN) / 100.0 * depth;
	}

	private static void use(ServerPlayer player, double used) {
		UUID id = player.getUUID();
		double saturation = saturation(player);
		if (saturation > 0) {
			double fromSaturation = Math.min(saturation, used);
			setSaturation(player, saturation - fromSaturation);
			used -= fromSaturation;
		}
		if (!Double.isFinite(used)) {
			return;
		}
		double total = USED.getOrDefault(id, 0.0) + used;
		if (total >= 1) {
			int whole = (int) total;
			total -= whole;
			set(player, get(player) - whole);
		}
		USED.put(id, total);
	}

	static void syncFoodSaturation(ServerPlayer player) {
		double value = Math.round(player.getFoodData().getSaturationLevel() * 4) / 4.0;
		Double shown = ((AttachmentTarget) player).getAttached(FOOD_SATURATION);
		if (shown == null || shown != value) {
			((AttachmentTarget) player).setAttached(FOOD_SATURATION, value);
		}
	}

	public static double shownFoodSaturation(Player player) {
		Double value = ((AttachmentTarget) player).getAttached(FOOD_SATURATION);
		return value != null ? value : 0;
	}

	static void update(ServerPlayer player) {
		UUID id = player.getUUID();
		if (!active(player)) {
			USED.remove(id);
			DRY.remove(id);
			return;
		}
		// Per minute -> per second
		double used = HardmodeConfig.get(FeatureSetting.THIRST_IDLE) / 60.0;
		if (player.isSprinting()) {
			used += HardmodeConfig.get(FeatureSetting.THIRST_SPRINT) / 60.0;
		}
		if (HardmodeConfig.isOn(FeatureSetting.THIRST_HEAT) && hot(player)) {
			used *= 2;
		}
		use(player, used * firstDayFactor(player));

		if (get(player) > 0) {
			DRY.remove(id);
			return;
		}
		int ticks = DRY.merge(id, 20, Integer::sum);
		int delay = (int) Math.round(HardmodeConfig.get(FeatureSetting.THIRST_DELAY) * 1200);
		int interval = (int) Math.max(20, Math.round(HardmodeConfig.get(FeatureSetting.THIRST_INTERVAL) * 20));
		double damage = HardmodeConfig.get(FeatureSetting.THIRST_DAMAGE);
		if (ticks >= delay && damage > 0 && (ticks - delay) % interval < 20) {
			ServerLevel level = player.level();
			player.hurtServer(level, player.damageSources().dryOut(), (float) damage);
		}
	}

	private static boolean hot(ServerPlayer player) {
		ServerLevel level = player.level();
		return level.dimension() == Level.NETHER
				|| level.getBiome(player.blockPosition()).value().getBaseTemperature() >= HOT;
	}
}
